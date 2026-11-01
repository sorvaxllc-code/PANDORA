package com.jarvis.kisisel;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.media.AudioAttributes;
import android.speech.tts.UtteranceProgressListener;
import java.util.Locale;

/** Reads a Quick Settings analysis result only on headphones; notifications always work. */
public final class PandoraHeadphoneReply {
    private PandoraHeadphoneReply() {}
    public static void say(Context context,String response) {
        Context app=context.getApplicationContext();
        if(!app.getSharedPreferences("jarvis_settings",Context.MODE_PRIVATE).getBoolean("voice",true)
            || !PandoraAlerts.headsetConnected(app) || PandoraAlerts.inCall(app))return;
        final Handler main=new Handler(Looper.getMainLooper());
        final TextToSpeech[] engine=new TextToSpeech[1];
        engine[0]=new TextToSpeech(app,status -> main.post(() -> {
            TextToSpeech tts=engine[0];
            if(tts==null)return;
            if(status!=TextToSpeech.SUCCESS) { tts.shutdown();return; }
            int language=tts.setLanguage(Locale.forLanguageTag("tr-TR"));
            if(language==TextToSpeech.LANG_NOT_SUPPORTED || language==TextToSpeech.LANG_MISSING_DATA
                || !PandoraAlerts.headsetConnected(app) || PandoraAlerts.inCall(app)) {
                tts.shutdown(); return;
            }
            tts.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build());
            final boolean[] ended={false};
            final Runnable[] watch={null};
            final Runnable close=() -> {
                if(ended[0])return;
                ended[0]=true;
                if(watch[0]!=null)main.removeCallbacks(watch[0]);
                tts.stop();
                tts.shutdown();
            };
            watch[0]=new Runnable() {
                @Override public void run() {
                    if(ended[0])return;
                    if(!PandoraAlerts.headsetConnected(app) || PandoraAlerts.inCall(app)
                        || !app.getSharedPreferences("jarvis_settings",Context.MODE_PRIVATE)
                                .getBoolean("voice",true)) {
                        close.run();
                    } else {
                        main.postDelayed(this,250);
                    }
                }
            };
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String id) {}
                @Override public void onDone(String id) { main.post(close); }
                @Override public void onError(String id) { main.post(close); }
            });
            String spoken=response.length()>375
                ?response.substring(0,365)+"... Devamı bildirimde."
                :response;
            if(tts.speak(spoken,TextToSpeech.QUEUE_FLUSH,null,"pandora-tile")
                    !=TextToSpeech.SUCCESS) {
                close.run();
                return;
            }
            main.postDelayed(watch[0],250);
            main.postDelayed(close,30000);
        }));
    }
}
