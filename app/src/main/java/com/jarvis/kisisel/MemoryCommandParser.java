package com.jarvis.kisisel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Small, local, deterministic command parser. No model or network access.
 * Keep it pure Java so the command logic can be tested without Android.
 */
public final class MemoryCommandParser {
    private static final Pattern PREFIX = Pattern.compile(
        "(?iu)^(?:(?:hey|merhaba)\\s+)?pandora\\s*[,!?:;.]?\\s*");
    private static final Pattern SAVE = Pattern.compile(
        "(?iu)^(?:(?:şu|bu|bunu|şunu)\\s+)?(?:(?:bilgiyi|bilgiyi\\s+lütfen)\\s+)?"+
        "(?:hafızana|hafızaya|hafızanda)\\s+(?:kaydet|yaz|ekle|not\\s+et)\\s*[:：,;.!-]?\\s*(.*)$");
    private static final Pattern REMEMBER = Pattern.compile(
        "(?iu)^(?:(?:şunu|bunu|bu\\s+bilgiyi)\\s+)?hatırla\\s*[:：,;.!-]?\\s*(.*)$");

    private MemoryCommandParser() {}

    /** Null if not an explicit memory command; empty string means no note supplied. */
    public static String extractSaveNote(String original) {
        if (original == null) return null;
        // Speech recognizers/soft keyboards may introduce newlines and irregular spaces.
        String command=original.replace('\u00a0',' ').replaceAll("\\s+", " ").trim();
        command=PREFIX.matcher(command).replaceFirst("").trim();
        Matcher m=SAVE.matcher(command);
        if (!m.matches()) m=REMEMBER.matcher(command);
        if (!m.matches()) return null;
        return m.group(1).trim();
    }
}
