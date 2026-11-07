package com.jarvis.kisisel;

/** Screen contents need distinct authorization before third-party cloud transfer. */
public final class ScreenCloudPolicy {
    private ScreenCloudPolicy() {}
    public static boolean allowed(String mode, boolean screenConsent) {
        if ("LOCAL".equals(mode)) return true;
        return ("CLOUD".equals(mode) || "AUTO".equals(mode) || "GROQ".equals(mode) || "HYBRID".equals(mode) || "HERMES".equals(mode)) && screenConsent;
    }
}
