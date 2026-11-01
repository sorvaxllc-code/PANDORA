package com.jarvis.kisisel;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
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
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String id) {}
                @Override public void onDone(String id) { main.post(tts::shutdown); }
                @Override public void onError(String id) { main.post(tts::shutdown); }
            });
            String spoken=response.length()>375
                ?response.substring(0,365)+"... Devamı bildirimde."
                :response;
            if(tts.speak(spoken,TextToSpeech.QUEUE_FLUSH,null,"pandora-tile")!=TextToSpeech.SUCCESS)
                tts.shutdown();
            main.postDelayed(tts::shutdown,30000);
        }));
    }
}
