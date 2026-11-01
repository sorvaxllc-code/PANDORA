package com.jarvis.kisisel;

/** Only for replies to explicitly spoken "Hey Pandora" commands.
 *  Unsolicited alerts still use their own headphones-only policy. */
public final class WakeVoicePolicy {
    private WakeVoicePolicy() {}
    public static boolean allowDirectSpeech(boolean voiceEnabled, boolean inCall) {
        return voiceEnabled && !inCall;
    }
}
