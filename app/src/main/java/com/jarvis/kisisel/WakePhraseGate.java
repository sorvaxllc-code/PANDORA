package com.jarvis.kisisel;

import java.util.Locale;

/** Deterministic second-stage verification of an offline Vosk hotword result. */
public final class WakePhraseGate {
    private WakePhraseGate(){}
    public static boolean isWakeText(String raw){
        if(raw==null)return false;
        String s=raw.toLowerCase(new Locale("tr","TR"))
            .replaceAll("[^\\p{L}\\s]"," ")
            .replaceAll("\\s+"," ").trim();
        return s.equals("hey pandora") || s.equals("hi pandora")
            || s.equals("hey pan dora") || s.equals("hey pandora hey pandora");
    }
    /**
     * Do not activate on partial transcripts. A completed utterance must have
     * plausible length and confidence. confidence < 0 means word scores were
     * unavailable; in that case a longer utterance is required.
     */
    public static boolean verified(String finalText,double confidence,long voicedMs){
        if(!isWakeText(finalText))return false;
        if(voicedMs<350 || voicedMs>5500)return false;
        return confidence>=0.58 || (confidence<0 && voicedMs>=680);
    }
}