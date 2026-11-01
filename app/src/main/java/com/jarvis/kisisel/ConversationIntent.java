package com.jarvis.kisisel;

import java.util.Locale;

/** Pure, testable intent classifier. Greeting must be the whole utterance. */
public final class ConversationIntent {
    private ConversationIntent() {}

    public static boolean isOnlyGreeting(String utterance) {
        if (utterance == null) return false;
        String normalized = utterance.toLowerCase(Locale.forLanguageTag("tr-TR"))
            .replaceAll("[\\p{Punct}…]+", " ")
            .replaceAll("\\s+", " ")
            .trim();
        // Greetings with other meaningful content MUST be sent to the selected model.
        return normalized.equals("merhaba") || normalized.equals("selam")
            || normalized.equals("merhaba pandora") || normalized.equals("selam pandora")
            || normalized.equals("hey pandora merhaba") || normalized.equals("hey pandora selam")
            || normalized.equals("günaydın") || normalized.equals("günaydın pandora")
            || normalized.equals("iyi akşamlar") || normalized.equals("iyi akşamlar pandora");
    }
}
