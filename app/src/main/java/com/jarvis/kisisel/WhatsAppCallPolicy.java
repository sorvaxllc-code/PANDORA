package com.jarvis.kisisel;

import java.util.Locale;

/** Conservative labels for a user-requested WhatsApp call upgrade, never a new call. */
public final class WhatsAppCallPolicy {
    private WhatsAppCallPolicy() {}

    public static boolean supportedPackage(String pkg) {
        return "com.whatsapp".equals(pkg) || "com.whatsapp.w4b".equals(pkg);
    }

    private static String norm(String label) {
        return label==null ? "" : label.trim().toLowerCase(new Locale("tr","TR"))
            .replaceAll("\\s+", " ");
    }

    public static boolean isEndCallControl(String label) {
        String s=norm(label);
        return s.contains("aramayı bitir") || s.contains("aramayı sonlandır")
            || s.contains("arama sonlandır") || s.contains("end call")
            || s.contains("hang up") || s.contains("end voice call");
    }

    public static boolean isAudioControl(String label) {
        String s=norm(label);
        return s.contains("sessize al") || s.contains("mute")
            || s.contains("hoparlör") || s.contains("speaker")
            || s.contains("sesi aç");
    }

    public static boolean indicatesActiveVideo(String label) {
        String s=norm(label);
        return s.contains("switch to voice") || s.contains("sesli aramaya geç")
            || s.contains("turn off video") || s.contains("stop video")
            || s.contains("kamerayı kapat") || s.contains("turn off camera");
    }

    /**
     * Generic 'video call' must belong to an actual button description, never
     * a headline, a chat message or an unclickable screen label.
     */
    public static boolean isUpgradeButton(String label, boolean isControl) {
        String s=norm(label);
        if(!isControl || s.isEmpty() || indicatesActiveVideo(s))return false;
        return s.contains("görüntülü görüşmeye geç")
            || s.contains("görüntülü aramaya geç")
            || s.contains("switch to video")
            || s.contains("turn on video")
            || s.equals("görüntülü arama")
            || s.equals("görüntülü görüşme")
            || s.equals("video call")
            || s.equals("video");
    }

    public static boolean mayUpgrade(String foregroundPkg,
                                     boolean hasEndCall,
                                     boolean hasAudioControl,
                                     boolean alreadyVideo) {
        return supportedPackage(foregroundPkg) && hasEndCall && hasAudioControl && !alreadyVideo;
    }
}
