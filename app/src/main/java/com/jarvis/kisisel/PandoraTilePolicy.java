package com.jarvis.kisisel;

/** No screen capture if user has not enabled both prerequisites. */
public final class PandoraTilePolicy {
    private PandoraTilePolicy() {}
    public static boolean canAnalyzeFromTile(boolean wakeRunning,boolean screenServiceReady) {
        return wakeRunning && screenServiceReady;
    }
}
