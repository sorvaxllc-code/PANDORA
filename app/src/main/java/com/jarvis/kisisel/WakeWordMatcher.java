package com.jarvis.kisisel;
import java.util.Locale;
/** Lightweight prefix check; only used after Android speech recognition returns text. */
public final class WakeWordMatcher {
    private WakeWordMatcher() {}
    public static boolean matches(String utterance) {
        if (utterance == null) return false;
        String value = utterance.toLowerCase(new Locale("tr", "TR"))
            .replaceAll("[.,!?;:]+", " ").replaceAll("\\s+", " ").trim();
        return value.equals("pandora") || value.equals("hey pandora") ||
            value.startsWith("pandora ") || value.startsWith("hey pandora ");
    }
}