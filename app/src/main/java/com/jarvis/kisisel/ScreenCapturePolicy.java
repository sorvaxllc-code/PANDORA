package com.jarvis.kisisel;

/**
 * A screen capture is bound to the exact foreground application present when
 * the user requested it. Reject stale OCR results after an app switch.
 * Does not grant permission; the AccessibilityService and Android still do that.
 */
public final class ScreenCapturePolicy {
    private ScreenCapturePolicy() {}

    public static boolean canUse(String requestedPackage, String currentPackage,
                                 boolean hasVisiblePassword) {
        if(requestedPackage==null || currentPackage==null) return false;
        if(!requestedPackage.equals(currentPackage)) return false;
        return ScreenSourcePolicy.canRead(currentPackage,hasVisiblePassword);
    }
}
