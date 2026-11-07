package com.jarvis.kisisel.safety;

import java.util.Locale;

/** Strong single-utterance check, including the Turkish Vosk emergency decoder.
 * A partial hypothesis can NEVER call this gate. */
public final class StopPhraseGate {
    private StopPhraseGate(){}
    public static boolean exact(String phrase){
        if(phrase==null)return false;
        String s=phrase.toLowerCase(new Locale("tr","TR"))
            .replaceAll("[^\\p{L}\\s]"," ").replaceAll("\\s+"," ").trim();
        return s.equals("dur")||s.equals("iptal et")
            ||s.equals("pandora dur")||s.equals("hey pandora dur");
    }
    public static boolean verified(String finalText,double confidence,long voicedMs){
        return exact(finalText) && voicedMs>=220 && voicedMs<=3600
            && confidence>=0.82;
    }
}
