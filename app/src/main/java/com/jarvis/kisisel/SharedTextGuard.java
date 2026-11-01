package com.jarvis.kisisel;

/** A shared URL alone is not evidence that the linked article was read. */
public final class SharedTextGuard {
    private SharedTextGuard() {}
    public static boolean onlyLink(String input) {
        if (input == null) return false;
        String stripped = input.trim()
            .replaceAll("(?i)https?://\\S+", "")
            .replaceAll("(?iu)[\\s\\p{Punct}]+", " ").trim();
        return input.matches("(?s).*https?://\\S+.*") && stripped.length() < 28;
    }
    public static String safeText(String input) {
        if (input == null) return "";
        String text = input.trim();
        return text.length() > 6000 ? text.substring(0,6000) : text;
    }
}
