package com.jarvis.kisisel;

/** Direct wake-command responses speak only through connected headphones.
 *  Without headphones, callers still receive a notification. */
public final class WakeVoicePolicy {
    private WakeVoicePolicy() {}
    public static boolean allowDirectSpeech(boolean voiceEnabled,
                                            boolean headsetConnected, boolean inCall) {
        return voiceEnabled && headsetConnected && !inCall;
    }
}
