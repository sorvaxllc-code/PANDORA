package com.jarvis.kisisel;
/** Process-local presentation state; no microphone audio or API credentials are retained. */
public final class VoiceSessionState {
    public static volatile String phase="Kapalı", transcript="", answer="";
    private VoiceSessionState(){}
    public static void set(String value){phase=value;}
}
