package com.jarvis.kisisel;
import android.content.Context;
import android.content.SharedPreferences;
import android.speech.tts.*;
/** Apply the user's actual installed voice consistently, without guessing voice gender. */
public final class SpeechPreferences {
    private SpeechPreferences(){}
    public static void apply(Context context,TextToSpeech engine){
        SharedPreferences p=context.getSharedPreferences("jarvis_settings",0);
        String selected=p.getString("pandora_voice_name","");
        java.util.Set<Voice> voices=engine.getVoices();
        if(voices!=null)for(Voice voice:voices)if(voice.getName().equals(selected)){engine.setVoice(voice);break;}
        engine.setPitch(p.getFloat("pandora_voice_pitch",0.85f));engine.setSpeechRate(p.getFloat("pandora_voice_rate",0.95f));
    }
}
