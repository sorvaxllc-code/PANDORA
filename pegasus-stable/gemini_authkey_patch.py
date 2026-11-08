#!/usr/bin/env python3
"""Patch PEGASUS Stable 1.0.3 to accept 2026 Gemini auth keys (AQ.) in 1.0.4.
Applied after gemini_direct_patch.py. Never logs or exports user credentials.
"""
from pathlib import Path

def replace(path, old, new):
    p=Path(path)
    s=p.read_text(encoding="utf-8")
    if s.count(old) != 1:
        raise RuntimeError("Missing or duplicated replacement anchor in "+str(p)+": "+old[:100])
    p.write_text(s.replace(old,new),encoding="utf-8")

p=Path("app/src/main/java/ai/pegasus/nexus/PegasusApiKeyVault.kt")
replace(p, '''if (v.length !in 30..160 || !v.startsWith("AIza") ||
            v.any { !(it.isLetterOrDigit() || it == '_' || it == '-') }) return false''',
'''val modern = v.startsWith("AQ.")
        val legacy = v.startsWith("AIza")
        // Google's 2026 auth keys can start with AQ. (dot included).
        // This is a syntax check only; Google validates the key on connect.
        if (v.length !in 30..256 || !(modern || legacy) ||
            v.any { !(it.isLetterOrDigit() || it == '_' || it == '-' || it == '.') }) return false''')

p=Path("app/src/main/java/ai/pegasus/nexus/PegasusGeminiLive.kt")
replace(p, '''if (apiKey.length !in 30..160 || !apiKey.startsWith("AIza")) {
            status("Google AI Studio API anahtarı gerekli · Ayarlar'dan kaydet"); return
        }''',
'''if (apiKey.length !in 30..256 ||
            !(apiKey.startsWith("AQ.") || apiKey.startsWith("AIza"))) {
            status("AI Studio API anahtarı (AQ. veya AIza) gerekli"); return
        }''')

p=Path("app/src/main/assets/nexus.html")
replace(p, '''placeholder="Google AI Studio API anahtarın (AIza...)"''',
'''placeholder="Google AI Studio API anahtarı (AQ. veya AIza...)"''')
replace(p, '''else "Anahtar kaydedilemedi · Google AI Studio anahtarını kontrol et"''',
'''else "Anahtar kaydedilemedi · AQ. veya AIza biçimini kontrol et"''') if False else None

p=Path("app/src/main/java/ai/pegasus/nexus/MainActivity.kt")
replace(p, '''else "Anahtar kaydedilemedi · Google AI Studio anahtarını kontrol et"''',
'''else "Anahtar kaydedilemedi · AQ. veya AIza biçimini kontrol et"''')

p=Path("app/build.gradle.kts")
replace(p,"versionCode = 503","versionCode = 504")
replace(p,'versionName = "1.0.3-direct-live-test"','versionName = "1.0.4-authkey-compat"')

gradle=Path("app/build.gradle.kts").read_text()
vault=Path("app/src/main/java/ai/pegasus/nexus/PegasusApiKeyVault.kt").read_text()
live=Path("app/src/main/java/ai/pegasus/nexus/PegasusGeminiLive.kt").read_text()
assert 'applicationId = "ai.pegasus.nexus.stable"' in gradle
assert 'signingConfigs.getByName("pegasusStable")' in gradle
assert "versionCode = 504" in gradle
assert "AQ." in vault and "AQ." in live
assert '"google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"' in live
assert "generationConfig" in live
print("PEGASUS_104_AUTHKEY_COMPAT_PATCH=PASS")
