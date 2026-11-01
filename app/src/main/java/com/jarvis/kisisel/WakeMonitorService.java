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
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import com.jarvis.kisisel.ai.ModelRouter;
import com.jarvis.kisisel.ai.HttpAiProvider;
import android.speech.tts.UtteranceProgressListener;
import java.util.ArrayList;
import java.util.Locale;

/**
 * User-initiated EXPERIMENTAL recognizer loop.
 * Android's system speech recognition is not a reliable always-on wake-word engine.
 * The installed speech service may process audio remotely, even with offline preference.
 */
public final class WakeMonitorService extends Service implements RecognitionListener {
    public static final String ACTION_START = "com.jarvis.kisisel.START_WAKE";
    public static final String ACTION_STOP = "com.jarvis.kisisel.STOP_WAKE";
    public static final String ACTION_SCREEN = "com.jarvis.kisisel.ANALYZE_CURRENT_SCREEN";
    public static final String ACTION_VIDEO = "com.jarvis.kisisel.UPGRADE_WHATSAPP_VIDEO";
    public static volatile boolean running = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private OfflineVoice offline;
    private boolean wakeShown;
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
    private boolean listening = false;
    private final WakeConversationState conversation = new WakeConversationState();
    private boolean processingAction = false;
    private HttpAiProvider.CancelToken pendingModel;
    private MemoryDb pendingDatabase;
    private int modelGeneration = 0;
    private final Runnable retry = this::listenOnce;

    @Override public void onCreate() {
        super.onCreate();
        tts = new TextToSpeech(this, status -> handler.post(() -> {
            if (stopped || tts == null || status != TextToSpeech.SUCCESS) return;
            int lang = tts.setLanguage(Locale.forLanguageTag("tr-TR"));
            ttsReady = lang != TextToSpeech.LANG_NOT_SUPPORTED &&
                lang != TextToSpeech.LANG_MISSING_DATA;
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
        offline=new OfflineVoice(this,new OfflineVoice.Listener(){
            public void partial(String phrase){
                if(stopped||speaking||processingAction)return;
                if(!wakeShown && WakeWordMatcher.matches(phrase)){wakeShown=true;OverlayPulse.show(WakeMonitorService.this);}
            }
            public void result(String phrase){
                if(stopped||speaking||processingAction)return;
                Bundle result=new Bundle();ArrayList<String> texts=new ArrayList<>();texts.add(phrase);
                result.putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION,texts);
                boolean handled=hear(result);wakeShown=false;
                if(handled && offline!=null)offline.pause();
            }
            public void error(){
                recordAnswer("Yerel ses dinlemesi başlatılamadı veya kesildi. Mikrofon başka bir uygulama tarafından kullanılıyor olabilir. PANDORA içinden yeniden başlatabilirsin.");
                stopSelf();
            }
        });
        return START_NOT_STICKY;
    }
    private void listenOnce() {
        if(stopped||speaking||processingAction)return;
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
                if(!wakeShown){OverlayPulse.show(this);wakeShown=true;}
                recordUser(phrase);return analyzeForegroundScreen(phrase);
            }
            if (kind == WakeQuickCommand.Kind.VIDEO_UPGRADE) {
                if(!wakeShown){OverlayPulse.show(this);wakeShown=true;}
                String message = DeviceTaskRouter.videoOptions(this);
                recordUser(phrase);recordAnswer(message);
                return speakDirectResponse(message);
            }
            recordUser(phrase);
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
                    if(!wakeShown){OverlayPulse.show(this);wakeShown=true;}
                    return false;
                default:
                    if(!wakeShown){OverlayPulse.show(this);wakeShown=true;}
                    return answerGeneralQuestion(phrase);
            }
            if(!wakeShown){OverlayPulse.show(this);wakeShown=true;}
            recordAnswer(response);
            // Spoken replies stay headphone-only, as requested by the user.
            return speakDirectResponse(response);
        }
        return false;
    }
    /** Any open-ended answer is informational. No model-generated action is executed in the background. */
    private boolean answerGeneralQuestion(String phrase) {
        String prompt=phrase.trim().replaceFirst("(?iu)^(?:hey\\s+)?pandora[\\s,.:;!?]*","").trim();
        String note=MemoryCommandParser.extractSaveNote(phrase);
        if(note!=null && note.isEmpty())return finishScreenAction("PANDORA","Ne not almamı istiyorsun? Hey Pandora diyerek notunu söyleyebilirsin.");
        if(note!=null){
            try(MemoryDb db=new MemoryDb(this)){db.note(note);}
            return finishScreenAction("PANDORA","Notunu kaydettim: "+note);
        }
        String task=DeviceTaskRouter.tryHandle(this,prompt);
        if(task!=null)return finishScreenAction("PANDORA",task);
        if(prompt.isEmpty())return finishScreenAction("PANDORA","Dinliyorum, ne yapmak istiyorsun?");
        return startModelQuery("Kullanıcı sana kulaklıktan sesleniyor. Türkçe ve kısa cevap ver. "
            +"Telefonu veya diğer uygulamaları kendi başına kontrol etme. Eylem JSON'u üretme. "
            +"Yetkisiz bir işi yaptığını iddia etme. Kullanıcının sorusu: "+prompt,"PANDORA · Yanıt");
    }

    private void runNotificationAction(String action) {
        if(stopped || processingAction) return;
        processingAction=true;
        // The mic is often unavailable during WhatsApp calls. Notification shortcuts
        // do not require speech recognition and still obey the accessibility permission.
        handler.removeCallbacks(retry);
        if(offline!=null)offline.pause();
        if(!wakeShown){OverlayPulse.show(this);wakeShown=true;}
        // Tapping a notification can leave Android's notification shade on top of X.
        // Dismiss only the shade when it is actually visible; do not leave the app.
        boolean shadeDismissed=PandoraAccessibilityService.dismissNotificationShadeIfVisible();
        handler.postDelayed(() -> {
            if(stopped) return;
            if(ACTION_VIDEO.equals(action)) {
                String msg=DeviceTaskRouter.videoOptions(this);
                finishScreenAction("PANDORA · WhatsApp",msg);
            } else if(ACTION_SCREEN.equals(action)) {
                analyzeForegroundScreen();
            }
        }, shadeDismissed ? 650 : 150);
    }
    private boolean analyzeForegroundScreen() { return analyzeForegroundScreen("Ekranı yorumla"); }
    private boolean analyzeForegroundScreen(String question) {
        processingAction=true;
        PandoraScreenAnalysisController.request(this,question,answer -> handler.post(() -> {
            if(stopped)return;
            processingAction=false;
            if(!speakDirectResponse(answer))restart(900);
        }));
        return true;
    }

    private boolean startModelQuery(String prompt,String title) {
        if(stopped || (pendingModel!=null))return false;
        pendingDatabase=new MemoryDb(this);
        ModelRouter router=new ModelRouter(this,pendingDatabase);
        if("OFF".equals(router.mode())) {
            closePendingDatabase();
            return finishScreenAction(title,
                "Bu soruyu yanıtlamak için PANDORA'da yerel veya bulut yapay zekâ modunu açmalısın.");
        }
        processingAction=true;
        
        final int generation=++modelGeneration;
        try {
            pendingModel=router.askInformationalAsync(prompt,(answer,source)->handler.post(()->{
                if(stopped || generation!=modelGeneration)return;
                pendingModel=null;
                closePendingDatabase();
                finishScreenAction(title,answer==null?"Yanıt alınamadı.":answer);
            }));
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
        recordAnswer(message);
        if(!speakDirectResponse(message))restart(900);
        return true;
    }
    private boolean speakDirectResponse(String response) {
        SharedPreferences settings = getSharedPreferences("jarvis_settings", MODE_PRIVATE);
        if (!WakeVoicePolicy.allowDirectSpeech(settings.getBoolean("voice", true),
                PandoraAlerts.headsetConnected(this), PandoraAlerts.inCall(this)) || !ttsReady || tts == null) return false;
        speaking = true;
        if(offline!=null)offline.pause();
        String spoken=response.length()>380
            ? response.substring(0,370)+"... Devamını sohbet geçmişinde okuyabilirsin."
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
    @Override public void onReadyForSpeech(Bundle params) {}
    @Override public void onBeginningOfSpeech() {}
    @Override public void onRmsChanged(float rmsdB) {}
    @Override public void onBufferReceived(byte[] buffer) {}
    @Override public void onEndOfSpeech() {}
    @Override public void onError(int error) {
        listening = false;
        if(error==SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS){
            stopSelf();return;
        }
        if (speaking || processingAction) return;
        // A busy recognizer or other app using the microphone can interrupt listening.
        restart((error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
                 error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) ? 5000 : 1500);
    }
    @Override public void onResults(Bundle results) {
        listening = false;
        if (!hear(results)) restart(1900);
    }
    // Partial recognition is often just the wake word: wait for a final transcript.
    @Override public void onPartialResults(Bundle partialResults) { }
    @Override public void onEvent(int eventType, Bundle params) {}
    @Override public void onDestroy() {
        stopped = true; running = false; listening = false; speaking = false;
        processingAction=false;
        conversation.reset();
        ++modelGeneration;
        if(pendingModel!=null)pendingModel.cancel();
        pendingModel=null;
        closePendingDatabase();
        handler.removeCallbacksAndMessages(null);
        if(offline!=null){offline.close();offline=null;}
        if (tts != null) { tts.stop(); tts.shutdown(); tts = null; }
        super.onDestroy();
    }
    private void recordUser(String text){try(MemoryDb db=new MemoryDb(this)){db.message("user",text);}}
    private void recordAnswer(String text){try(MemoryDb db=new MemoryDb(this)){db.message("pandora",text);}}
    @Override public IBinder onBind(Intent intent) { return null; }
}