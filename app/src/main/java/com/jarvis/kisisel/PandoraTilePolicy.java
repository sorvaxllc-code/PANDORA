package com.jarvis.kisisel;

/** Quick Settings uses the independently authorized AccessibilityService, not the mic. */
public final class PandoraTilePolicy {
    private PandoraTilePolicy() {}
    public static boolean canAnalyzeFromTile(boolean screenServiceReady) {
        return screenServiceReady;
    }
}
