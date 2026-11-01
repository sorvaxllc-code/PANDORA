package com.jarvis.kisisel;

import java.util.Locale;

/**
 * Conservative, local guard before on-demand reading.
 * Not a promise to detect every sensitive app; excludes PANDORA, system panels,
 * and screens containing password input nodes.
 */
public final class ScreenSourcePolicy {
    private ScreenSourcePolicy() {}
    public static boolean canRead(String foregroundPackage,boolean passwordInputVisible) {
        if(passwordInputVisible || foregroundPackage==null)return false;
        String pkg=foregroundPackage.trim().toLowerCase(Locale.ROOT);
        if(pkg.isEmpty() || pkg.equals("null"))return false;
        if(pkg.startsWith("com.jarvis.kisisel"))return false;
        if(pkg.equals("com.android.systemui") || pkg.equals("android"))return false;
        return true;
    }
}
