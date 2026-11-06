package com.jarvis.kisisel;
import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.media.AudioAttributes;
import android.content.SharedPreferences;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import com.jarvis.kisisel.ai.ModelRouter;
import com.jarvis.kisisel.ai.HttpAiProvider;
import android.speech.tts.UtteranceProgressListener;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Offline continuous headset hotword service. It never calls Android SpeechRecognizer.
 * A foreground microphone indicator is required by Android; spoken requests are
 * forwarded to Hermes only after a verified "Hey Pandora" and command.
 */
public final class WakeMonitorService extends Service {
    public static final String ACTION_START = "com.jarvis.kisisel.START_WAKE";
    public static final String ACTION_STOP = "com.jarvis.kisisel.STOP_WAKE";
    public static final String ACTION_SCREEN = "com.jarvis.kisisel.ANALYZE_CURRENT_SCREEN";
    public static final String ACTION_VIDEO = "com.jarvis.kisisel.UPGRADE_WHATSAPP_VIDEO";
    public static volatile boolean running = false;
    public static volatile String microphoneState="Kapalı";
    public static volatile long lastWakeMs=0L;
    public static volatile String lastError="";
    public static volatile boolean audioReady=false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private OfflineVoice offline;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private boolean speaking = false;
    private boolean stopped = false;
    private final Runnable speechTimeout = () -> {
        if (!stopped && speaking) {
            speaking = false;
            if (tts != null) tts.stop();
            // The next headphone monitor tick sees speaking=false and exits.
            restart(800);
        }
    };
    // Speech must stop if Bluetooth/wired headphones disconnect mid-reply.
    // Android may otherwise automatically reroute audio to the phone speaker.
    private final Runnable headphoneGuard = new Runnable() {
        @Override public void run() {
            if(stopped || !speaking)return;
            if(!PandoraAlerts.headsetConnected(WakeMonitorService.this)
                || PandoraAlerts.inCall(WakeMonitorService.this)) {
                speaking=false;
                if(tts!=null)tts.stop();
                handler.removeCallbacks(speechTimeout);
                restart(700);
                return;
            }
            handler.postDelayed(this,250);
        }
    };
    private boolean headsetPaused = false;
    private final WakeConversationState conversation = new WakeConversationState();
    private boolean processingAction = false;
    private HttpAiProvider.CancelToken pendingModel;
    private com.jarvis.kisisel.core.CaptainTeam voiceTeam;
    private PandoraCallResolver.Match pendingVoiceCall;
    private long pendingVoiceCallExpiry=0;
    private MemoryDb pendingDatabase;
    private int modelGeneration = 0;
    private final Runnable retry = this::listenOnce;
    // Stop hotword detection and TTS promptly when headphones disconnect.
    // An enabled monitor stays alive but CAPTURES NO AUDIO when headphones disconnect
    // or a phone call is active. It resumes after headphones reconnect.
    private final Runnable headsetSafety=new Runnable(){
        @Override public void run(){
            if(stopped)return;
            boolean allowed=PandoraAlerts.headsetConnected(WakeMonitorService.this)
                && !PandoraAlerts.inCall(WakeMonitorService.this);
            if(!allowed && !headsetPaused){
                headsetPaused=true;
                handler.removeCallbacks(retry);
                if(offline!=null)offline.pause();
                if(tts!=null)tts.stop();
                speaking=false;
                handler.removeCallbacks(headphoneGuard);
                handler.removeCallbacks(speechTimeout);
            }else if(allowed && headsetPaused){
                headsetPaused=false;
                if(!processingAction && !speaking)restart(450);
            }
            handler.postDelayed(this,500);
        }
    };
    @Override public void onCreate() {
        super.onCreate();
        tts = new TextToSpeech(this, status -> handler.post(() -> {
            if (stopped || tts == null || status != TextToSpeech.SUCCESS) return;
            int lang = tts.setLanguage(Locale.forLanguageTag("tr-TR"));
            ttsReady = lang != TextToSpeech.LANG_NOT_SUPPORTED &&
                lang != TextToSpeech.LANG_MISSING_DATA;
            if(ttsReady){
                android.content.SharedPreferences prefs=getSharedPreferences("jarvis_settings",MODE_PRIVATE);
                tts.setPitch(prefs.getFloat("pandora_voice_pitch",0.85f));
                tts.setSpeechRate(prefs.getFloat("pandora_voice_rate",0.95f));
                String preferred=prefs.getString("pandora_voice_name","");
                if(!preferred.isEmpty() && tts.getVoices()!=null)for(android.speech.tts.Voice v:tts.getVoices()){
                    if(v!=null&&preferred.equals(v.getName())){tts.setVoice(v);break;}
                }
            }
            if (!ttsReady) return;
            tts.setAudioAttributes(new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_ASSISTANT).build());
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String utteranceId) {}
                @Override public void onDone(String utteranceId) { onSpeechFinished(); }
                @Override public void onError(String utteranceId) { onSpeechFinished(); }
            });
        }));
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf(); return START_NOT_STICKY;
        }
        if(intent != null && (ACTION_SCREEN.equals(intent.getAction()) ||
                ACTION_VIDEO.equals(intent.getAction()))) {
            if(running && !stopped) {
                String action=intent.getAction();
                handler.post(()->runNotificationAction(action));
            } else {
                PandoraAlerts.postImportant(this,"PANDORA","Önce PANDORA içinden Hey Pandora dinlemesini başlat.");
            }
            return START_NOT_STICKY;
        }
        if (running) return START_NOT_STICKY;
        // Start is allowed only from a visible activity with microphone permission;
        // if the headset disappears later, remain paused without capturing sound.
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            stopSelf(); return START_NOT_STICKY;
        }
        stopped = false;
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                startForeground(2044, PandoraAlerts.wakeNotification(this),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
            } else {
                startForeground(2044, PandoraAlerts.wakeNotification(this));
            }
        } catch (RuntimeException e) {
            stopSelf(); return START_NOT_STICKY;
        }
        running = true;
        microphoneState="Çevrimdışı modeller hazırlanıyor";
        audioReady=false;
        lastError="";
        headsetPaused = !PandoraAlerts.headsetConnected(this) || PandoraAlerts.inCall(this);
        handler.postDelayed(headsetSafety,350);
        offline=new OfflineVoice(this,new OfflineVoice.Listener(){
            @Override public void wake(){
                if(stopped || headsetPaused || speaking || processingAction)return;
                lastWakeMs=android.os.SystemClock.elapsedRealtime();
                microphoneState="Hey Pandora algılandı · komut dinleniyor";
                OverlayPulse.show(WakeMonitorService.this);
                if(getSharedPreferences("jarvis_settings",MODE_PRIVATE)
                    .getBoolean("pandora_wake_feedback",true))
                    WakeTone.play(WakeMonitorService.this);
                // Only the verified wake phrase triggers one original soft cue.
                // Ambient sound, model loading and STT restarts play nothing.
            }
            @Override public void result(String phrase){
                if(stopped || headsetPaused || speaking || processingAction)return;
                Bundle result=new Bundle();
                ArrayList<String> values=new ArrayList<>();
                values.add(phrase);
                result.putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION,values);
                hear(result);
                if(offline!=null)offline.pause();
                if(!speaking&&!processingAction)restart(650);
            }
            @Override public void status(String state){
                if(stopped)return;
                microphoneState=state;
                audioReady=state.contains("Kulaklık mikrofonu etkin")
                    || state.contains("Hey Pandora için hazır")
                    || state.contains("komutunu dinliyorum")
                    || state.contains("Komut alındı");
            }
            @Override public void error(String reason){
                if(stopped)return;
                microphoneState=reason;
                lastError=reason;
                audioReady=false;
                PandoraAlerts.postImportant(WakeMonitorService.this,
                    "PANDORA · Ses sistemi",reason);
                stopSelf();
            }
        });
        if(!headsetPaused)offline.resume();
        // Android 13 and earlier may restart an explicitly enabled foreground
        // wake service after memory pressure. Android 14+ enforces additional
        // microphone while-in-use startup restrictions; do not auto-restart there.
        return Build.VERSION.SDK_INT<=33 ? START_STICKY : START_NOT_STICKY;
    }
    private void listenOnce(){
        if(stopped||headsetPaused||speaking||processingAction)return;
        if(!PandoraAlerts.headsetConnected(this)||PandoraAlerts.inCall(this))return;
        if(offline!=null)offline.resume();
    }
    private void restart(long delayMs) {
        if (stopped) return;
        handler.removeCallbacks(retry);
        handler.postDelayed(retry, delayMs);
    }
    private boolean hear(Bundle results) {
        if(results==null)return false;
        ArrayList<String> texts=results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if(texts==null)return false;
        for(String phrase:texts) {
            long now=android.os.SystemClock.elapsedRealtime();
            WakeQuickCommand.Kind kind=conversation.accept(phrase,now);
            if(kind==null)continue;
            if (kind == WakeQuickCommand.Kind.SCREEN_ANALYZE) {
                OverlayPulse.show(this);
                return analyzeForegroundScreen();
            }
            if (kind == WakeQuickCommand.Kind.VIDEO_UPGRADE) {
                OverlayPulse.show(this);
                String message = PandoraAccessibilityService.requestWhatsAppVideoSwitch();
                PandoraAlerts.postImportant(this,"PANDORA · WhatsApp",message);
                return speakDirectResponse(message);
            }
            String response;
            switch(kind) {
                case TIME:
                    response="Şu an saat "+new SimpleDateFormat("HH:mm",new Locale("tr","TR")).format(new Date())+".";
                    break;
                case DATE:
                    response="Bugün "+DateFormat.getDateInstance(DateFormat.FULL,new Locale("tr","TR")).format(new Date())+".";
                    break;
                case BATTERY:
                    Intent battery=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                    int level=battery==null?-1:battery.getIntExtra(BatteryManager.EXTRA_LEVEL,-1);
                    int scale=battery==null?-1:battery.getIntExtra(BatteryManager.EXTRA_SCALE,-1);
                    response=(level<0||scale<=0)?"Pil seviyesi şu anda okunamadı.":
                        "Telefonun şarjı yüzde "+Math.round(100.0f*level/scale)+".";
                    break;
                case WAKE_ONLY:
                    response="Dinliyorum. Şimdi ne yapmamı istediğini söyle.";
                    break;
                default:
                    OverlayPulse.show(this);
                    return answerGeneralQuestion(phrase);
            }
            OverlayPulse.show(this);
            // No wake beep and no notification for hearing "Hey Pandora" alone.
            if(kind!=WakeQuickCommand.Kind.WAKE_ONLY)
                PandoraAlerts.postImportant(this,"PANDORA · Sesli komut",response);
            // Spoken replies stay headphone-only, as requested by the user.
            return speakDirectResponse(response);
        }
        return false;
    }
    /** Any open-ended answer is informational. No model-generated action is executed in the background. */
    private boolean answerGeneralQuestion(String phrase) {
        String prompt=phrase.trim().replaceFirst("(?iu)^(?:hey\\s+)?pandora[\\s,.:;!?]*","").trim();
        if(prompt.isEmpty())return finishScreenAction("PANDORA","Dinliyorum, ne yapmak istiyorsun?");
        String low=prompt.toLowerCase(new Locale("tr","TR"));
        if(low.equals("aramayı onayla") || low.equals("aramayı başlat")
            || low.equals("ara onayla"))return confirmVoiceCall();
        if(low.equals("aramayı iptal et") || low.equals("arama iptal")){
            pendingVoiceCall=null;pendingVoiceCallExpiry=0;
            return finishScreenAction("PANDORA · Arama","Bekleyen arama iptal edildi.");
        }
        if(low.endsWith(" ara") && low.length()>5
            && !low.startsWith("internette ") && !low.startsWith("google ")
            && !low.startsWith("youtube ")){
            String target=prompt.replaceFirst("(?iu)\\s+ara$","")
                .replaceFirst("(?iu)^(?:telefonla|numarayı|kişiyi)\\s+","").trim();
            return prepareVoiceCall(target);
        }

        // Hands-free manager dispatch needs no touch or screen visibility.
        if(prompt.toLowerCase(new Locale("tr","TR")).matches(
                "^(?:ekibe|ekip|ekibime|müdür|müdüre)\\s+.{4,}$")) {
            String goal=prompt.replaceFirst(
                "(?iu)^(?:ekibe|ekip|ekibime|müdür|müdüre)\\s+","").trim();
            return startVoiceTeam(goal);
        }
        boolean explicitHermes="HYBRID".equals(
            getSharedPreferences("jarvis_settings",MODE_PRIVATE)
                .getString("pandora_mode","HERMES")) &&
            com.jarvis.kisisel.ai.HybridRoutePolicy.isExplicitHermesRequest(prompt);
        String plain=explicitHermes
            ? com.jarvis.kisisel.ai.HybridRoutePolicy.stripHermesPrefix(prompt):prompt;
        String question="Kullanıcı sana kulaklıktan sesleniyor. Türkçe ve kısa cevap ver. "
            +"Telefonu veya diğer uygulamaları kendi başına kontrol etme. Eylem JSON'u üretme. "
            +"Yetkisiz bir işi yaptığını iddia etme. Kullanıcının sorusu: "+plain;
        return startModelQuery(question,"PANDORA · Yanıt",explicitHermes);
    }

    private boolean prepareVoiceCall(String requested){
        if(PandoraAlerts.inCall(this))
            return finishScreenAction("PANDORA · Telefon","Görüşme devam ederken arama başlatamam.");
        if(PandoraCallResolver.phoneNumber(requested)==null &&
            checkSelfPermission(Manifest.permission.READ_CONTACTS)
                !=PackageManager.PERMISSION_GRANTED)
            return finishScreenAction("PANDORA · Telefon",
                "Kişiyi aramak için Kişiler erişimini PANDORA Kurulum Merkezi'nden açmalısın.");
        java.util.List<PandoraCallResolver.Match> matches=
            PandoraCallResolver.resolve(this,requested);
        if(matches.size()!=1){
            return finishScreenAction("PANDORA · Telefon",
                matches.isEmpty()?"Bu isimde kişi veya geçerli numara bulamadım. Arama yapılmadı.":
                    "Bu isimde birden çok numara var. Tam adıyla veya numarasıyla tekrar söyle.");
        }
        pendingVoiceCall=matches.get(0);
        pendingVoiceCallExpiry=android.os.SystemClock.elapsedRealtime()+35000L;
        return finishScreenAction("PANDORA · Sesli arama onayı",
            pendingVoiceCall.label+" kişisini "+pendingVoiceCall.number+
            " numarasından aramamı istedin. Aramayı başlatmak için "
            +"Pandora, aramayı onayla de. İptal etmek için Pandora, aramayı iptal et de.");
    }
    private boolean confirmVoiceCall(){
        PandoraCallResolver.Match target=pendingVoiceCall;
        pendingVoiceCall=null;
        long expiry=pendingVoiceCallExpiry;
        pendingVoiceCallExpiry=0L;
        if(target==null||android.os.SystemClock.elapsedRealtime()>expiry)
            return finishScreenAction("PANDORA · Telefon",
                "Onay bekleyen bir arama yok. Kimi arayacağımı tekrar söyle.");
        if(checkSelfPermission(Manifest.permission.CALL_PHONE)
            !=PackageManager.PERMISSION_GRANTED)
            return finishScreenAction("PANDORA · Telefon",
                "Telefon araması Android tarafından yetkilendirilmedi. "
                 +"PANDORA Kurulum Merkezi'nden arama izni vermen gerekiyor.");
        try{
            android.telecom.TelecomManager telecom=
                (android.telecom.TelecomManager)getSystemService(TELECOM_SERVICE);
            if(telecom==null)
                return finishScreenAction("PANDORA · Telefon",
                    "Android telefon servisi kullanılamıyor.");
            telecom.placeCall(android.net.Uri.fromParts("tel",target.number,null),
                new Bundle());
            return finishScreenAction("PANDORA · Telefon",
                "Android'e "+target.label+" için arama isteğini gönderdim. "
                +"Çağrının bağlandığını doğrulayamıyorum.");
        }catch(SecurityException | IllegalArgumentException | IllegalStateException e){
            return finishScreenAction("PANDORA · Telefon",
                "Android aramayı başlatmayı engelledi. "
                +"Telefon uygulamasından yeniden deneyebilirsin.");
        }
    }

    private boolean startVoiceTeam(String goal){
        if(stopped||processingAction)return false;
        processingAction=true;
        if(offline!=null)offline.pause();
        if(voiceTeam==null)voiceTeam=new com.jarvis.kisisel.core.CaptainTeam(this);
        if(voiceTeam.busy()){
            return finishScreenAction("PANDORA · Müdür",
                "Müdür hâlâ önceki görevi yürütüyor.");
        }
        PandoraAlerts.postImportant(this,"PANDORA · Uzmanlar çalışıyor",
            "Müdür işi uzmanlara ayırdı. Tamamlanınca sesli bildireceğim.");
        voiceTeam.start(goal,new com.jarvis.kisisel.core.CaptainTeam.Listener(){
            @Override public void progress(String label,String detail,int step){
                // Progress stays silent and is never presented as completed.
            }
            @Override public void complete(String task,String report){
                if(stopped)return;
                finishScreenAction("PANDORA · Müdür raporu",
                    "Müdür ve iki uzman yanıtlarını tamamladı. "+report);
            }
            @Override public void failed(String reason){
                if(stopped)return;
                finishScreenAction("PANDORA · Ekip görevi başarısız",
                    "Ekip çalışması tamamlanamadı. "+reason);
            }
        });
        return true;
    }

    private void runNotificationAction(String action) {
        if(stopped || processingAction) return;
        processingAction=true;
        // The mic is often unavailable during WhatsApp calls. Notification shortcuts
        // do not require speech recognition and still obey the accessibility permission.
        handler.removeCallbacks(retry);
        if(offline!=null)offline.pause();
        OverlayPulse.show(this);
        // Tapping a notification can leave Android's notification shade on top of X.
        // Dismiss only the shade when it is actually visible; do not leave the app.
        boolean shadeDismissed=PandoraAccessibilityService.dismissNotificationShadeIfVisible();
        handler.postDelayed(() -> {
            if(stopped) return;
            if(ACTION_VIDEO.equals(action)) {
                String msg=PandoraAccessibilityService.requestWhatsAppVideoSwitch();
                finishScreenAction("PANDORA · WhatsApp",msg);
            } else if(ACTION_SCREEN.equals(action)) {
                analyzeForegroundScreen();
            }
        }, shadeDismissed ? 650 : 150);
    }
    private boolean analyzeForegroundScreen() {
        processingAction=true;
        PandoraScreenAnalysisController.request(this,answer -> handler.post(() -> {
            if(stopped)return;
            processingAction=false;
            if(!speakDirectResponse(answer))restart(900);
        }));
        return true;
    }

    private boolean startModelQuery(String prompt,String title) {
        return startModelQuery(prompt,title,false);
    }
    private boolean startModelQuery(String prompt,String title,boolean forceHermes) {
        if(stopped || (pendingModel!=null))return false;
        pendingDatabase=new MemoryDb(this);
        ModelRouter router=new ModelRouter(this,pendingDatabase);
        if("OFF".equals(router.mode())) {
            closePendingDatabase();
            return finishScreenAction(title,
                "Bu soruyu yanıtlamak için PANDORA'da yerel veya bulut yapay zekâ modunu açmalısın.");
        }
        processingAction=true;
        // Preparing a reply is silent: Android keeps only the microphone FGS indicator.
        final int generation=++modelGeneration;
        try {
            ModelRouter.Callback callback=(answer,source)->handler.post(()->{
                if(stopped || generation!=modelGeneration)return;
                pendingModel=null;
                closePendingDatabase();
                finishScreenAction(title,answer==null?"Yanıt alınamadı.":answer);
            });
            pendingModel=forceHermes
                ? router.askHermesInformationalAsync(prompt,callback)
                : router.askInformationalAsync(prompt,callback);
        } catch(RuntimeException e) {
            pendingModel=null;
            closePendingDatabase();
            return finishScreenAction(title,"Yapay zekâ başlatılamadı.");
        }
        handler.postDelayed(()->{
            if(stopped || generation!=modelGeneration || !processingAction)return;
            ++modelGeneration;
            if(pendingModel!=null)pendingModel.cancel();
            pendingModel=null;
            closePendingDatabase();
            finishScreenAction(title,"Yapay zekâ zamanında yanıt vermedi; işlem durduruldu.");
        }, 90000);
        return true;
    }
    private void closePendingDatabase() {
        if(pendingDatabase!=null) {
            pendingDatabase.close();
            pendingDatabase=null;
        }
    }
    private boolean finishScreenAction(String title,String message) {
        processingAction=false;
        PandoraAlerts.postImportant(this,title,message);
        if(!speakDirectResponse(message))restart(900);
        return true;
    }
    private boolean speakDirectResponse(String response) {
        SharedPreferences settings = getSharedPreferences("jarvis_settings", MODE_PRIVATE);
        if (!WakeVoicePolicy.allowDirectSpeech(settings.getBoolean("voice", true),
                PandoraAlerts.headsetConnected(this), PandoraAlerts.inCall(this)) || !ttsReady || tts == null) return false;
        speaking = true;
        String spoken=response.length()>380
            ? response.substring(0,370)+"... Devamını bildirimde okuyabilirsin."
            : response;
        int result = tts.speak(spoken, TextToSpeech.QUEUE_FLUSH, null, "pandora-wake");
        if (result != TextToSpeech.SUCCESS) {
            speaking = false;
            return false;
        }
        handler.removeCallbacks(retry);
        handler.removeCallbacks(speechTimeout);
        handler.removeCallbacks(headphoneGuard);
        handler.postDelayed(headphoneGuard,250);
        handler.postDelayed(speechTimeout, 20000);
        return true;
    }
    private void onSpeechFinished() {
        handler.post(() -> {
            if (stopped || !speaking) return;
            speaking = false;
            handler.removeCallbacks(speechTimeout);
            handler.removeCallbacks(headphoneGuard);
            restart(650);
        });
    }
    @Override public void onDestroy() {
        stopped = true; running = false; speaking = false; headsetPaused = false;
        microphoneState="Kapalı";
        audioReady=false;
        processingAction=false;
        conversation.reset();
        ++modelGeneration;
        pendingVoiceCall=null;pendingVoiceCallExpiry=0L;
        if(voiceTeam!=null){voiceTeam.close();voiceTeam=null;}
        if(pendingModel!=null)pendingModel.cancel();
        pendingModel=null;
        closePendingDatabase();
        handler.removeCallbacksAndMessages(null);
        if(offline!=null){offline.close();offline=null;}
        if (tts != null) { tts.stop(); tts.shutdown(); tts = null; }
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}