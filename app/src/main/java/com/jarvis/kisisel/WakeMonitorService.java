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
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
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
    public static volatile boolean running = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private boolean listening = false;
    private long lastTriggerMs = 0;
    private boolean stopped = false;
    private final Runnable retry = this::listenOnce;

    @Override public void onCreate() {
        super.onCreate();
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS && tts != null) {
                int lang = tts.setLanguage(Locale.forLanguageTag("tr-TR"));
                ttsReady = lang != TextToSpeech.LANG_NOT_SUPPORTED &&
                    lang != TextToSpeech.LANG_MISSING_DATA;
            }
        });
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf(); return START_NOT_STICKY;
        }
        if (running) return START_NOT_STICKY;
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            stopSelf(); return START_NOT_STICKY;
        }
        stopped = false;
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(2044, PandoraAlerts.wakeNotification(this),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
            } else {
                startForeground(2044, PandoraAlerts.wakeNotification(this));
            }
        } catch (RuntimeException e) {
            stopSelf(); return START_NOT_STICKY;
        }
        running = true;
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            PandoraAlerts.postImportant(this,"PANDORA","Ses tanıma hizmeti bulunamadı; dinleme kapatıldı.");
            stopSelf(); return START_NOT_STICKY;
        }
        try {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this);
            recognizer.setRecognitionListener(this);
            handler.postDelayed(retry, 350);
        } catch (RuntimeException e) { stopSelf(); }
        return START_NOT_STICKY;
    }
    private void listenOnce() {
        if (stopped || recognizer == null || listening) return;
        try {
            Intent request = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            request.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            request.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR");
            request.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            request.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
            listening = true;
            recognizer.startListening(request);
        } catch (RuntimeException e) { listening = false; restart(3000); }
    }
    private void restart(long delayMs) {
        if (stopped) return;
        handler.removeCallbacks(retry);
        handler.postDelayed(retry, delayMs);
    }
    private void hear(Bundle results) {
        if(results==null)return;
        ArrayList<String> texts=results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if(texts==null)return;
        for(String phrase:texts) {
            WakeQuickCommand.Result result=WakeQuickCommand.parse(phrase);
            if(result==null)continue;
            long now=android.os.SystemClock.elapsedRealtime();
            if(now-lastTriggerMs<12000)return;
            lastTriggerMs=now;
            String response;
            switch(result.kind) {
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
                    response="Seni duydum. Saat, tarih veya şarj sorabilirsin. Diğer komutlar için bildirime dokun.";
                    break;
                default:
                    response="Seni duydum. Bu isteği arka planda çalıştırmıyorum. Uygulamayı açarak devam et.";
                    break;
            }
            PandoraAlerts.postImportant(this,"PANDORA · Sesli komut",response);
            if(ttsReady && PandoraAlerts.headsetConnected(this) && !PandoraAlerts.inCall(this)) {
                tts.speak(response,TextToSpeech.QUEUE_FLUSH,null,"pandora-wake");
            }
            return;
        }
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
        // A busy recognizer or other app using the microphone can interrupt listening.
        restart((error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
                 error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) ? 5000 : 1500);
    }
    @Override public void onResults(Bundle results) {
        hear(results); listening = false; restart(1900);
    }
    // Partial recognition is often just the wake word: wait for a final transcript.
    @Override public void onPartialResults(Bundle partialResults) { }
    @Override public void onEvent(int eventType, Bundle params) {}
    @Override public void onDestroy() {
        stopped = true; running = false; listening = false;
        handler.removeCallbacksAndMessages(null);
        if (recognizer != null) { recognizer.cancel(); recognizer.destroy(); recognizer = null; }
        if (tts != null) { tts.stop(); tts.shutdown(); tts = null; }
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}