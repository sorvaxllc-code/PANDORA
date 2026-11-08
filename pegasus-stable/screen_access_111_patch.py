#!/usr/bin/env python3
"""PEGASUS Stable 1.0.11: root-window resolution, consent and screen diagnostics.

Preserve NEXUS visuals, P hologram (SYSTEM_ALERT_WINDOW), Gemini voice and
Android-protected screens. No automatic accessibility enabling or screen capture.
Applied after 1.0.10 patch.
"""
from pathlib import Path

def rep(path, old, new):
    p=Path(path)
    s=p.read_text(encoding="utf-8")
    n=s.count(old)
    if n!=1:
        raise RuntimeError(f"{p}: expected one anchor, found {n}: {old[:115]}")
    p.write_text(s.replace(old,new),encoding="utf-8")

src=Path("app/src/main/java/ai/pegasus/nexus")
a=src/"PegasusAccessService.kt"
v=src/"PegasusLiveService.kt"
m=src/"MainActivity.kt"
g=src/"PegasusGeminiLive.kt"
html=Path("app/src/main/assets/nexus.html")

# Android can report an accessibility overlay or null as rootInActiveWindow.
# Prefer the focused, visible TYPE_APPLICATION window; never walk all
# background app windows. If none is focused, fail instead of bypassing.
rep(a,'''    private fun policyAppBlock(pkg: String): String? {''',
'''    private fun foregroundApplicationRoot(): android.view.accessibility.AccessibilityNodeInfo? {
        val topWindows=windows
        for (w in topWindows) {
            if (w.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION &&
                (w.isActive || w.isFocused)) {
                val root=w.root
                if (root!=null) return root
            }
        }
        // On some OEM versions getWindows() is not populated. Only fall
        // back to the actual active application, not an accessibility overlay.
        if (topWindows.isNotEmpty()) return null
        val activeRoot=rootInActiveWindow ?: return null
        val activeWindow=activeRoot.window
        if (activeWindow != null &&
            activeWindow.type != android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION)
            return null
        return activeRoot
    }

    /** This is a LOCAL read-only diagnostic, not an AI screen capture.
     * Package/status only; no labels or screen text leave this function.
     */
    fun screenAccessDiagnostics(): org.json.JSONObject {
        fun result(ok:Boolean,code:String,description:String) =
            org.json.JSONObject().put("ok",ok).put("code",code)
                .put("detail",description.take(230))
        if (stopped) return result(false,"SERVICE_PAUSED",
            "PEGASUS ekran kontrolü durduruldu. Ses oturumunda Telefon araçlarına izin ver.")
        if (consentWindow!=null) return result(false,"CONSENT_PENDING",
            "Ekranda PEGASUS izin penceresi açık. Onayla veya Reddet'e bas.")
        val root=foregroundApplicationRoot() ?: return result(false,"ANDROID_NO_WINDOW",
            "Android etkin uygulamanın erişilebilir penceresini vermiyor. " +
            "Erişilebilirlik hizmetini kontrol et; bazı görseller metin sunmaz.")
        val pkg=root.packageName?.toString().orEmpty()
        val blocker=policyAppBlock(pkg)
        if (blocker!=null) return result(false,"PEGASUS_PROTECTION",
            "Bu uygulama korumalı: "+blocker)
        if (!hasScreenAccess(pkg)) return result(false,"APP_NEEDS_CONSENT",
            "Bu uygulamanın metinlerini okumak için PEGASUS'un tek oturumluk onayı gerekli: " +
            pkg.take(75))
        return result(true,"READY",
            "Ekran okuma hazır: "+pkg.take(75)+
            ". Görsel/OCR içeriği ayrı izin gerektirir.")
    }

    private fun policyAppBlock(pkg: String): String? {''')

# Transparent permission overlay itself changes Android's focused window on
# some vendors. The user's explicit positive click approves the captured
# package only for this session; subsequent accesses still re-check active app.
rep(a,'''if (rootInActiveWindow?.packageName?.toString()!=pkg) {''',
'''if (foregroundApplicationRoot()?.packageName?.toString()!=pkg) {''')
rep(a,'''if (accepted && rootInActiveWindow?.packageName?.toString()==pkg &&
                    policyAppBlock(pkg)==null) {''',
'''if (accepted && consentPackage==pkg &&
                    policyAppBlock(pkg)==null) {''')

# While consent overlay is open, do not read that overlay or app text.
rep(a,'''        if (stopped) return fail("Ekran erişimi durmuş · oturum iznini tekrar ver")
        val root=rootInActiveWindow ?:''',
'''        if (stopped) return org.json.JSONObject().put("ok",false)
            .put("blocker","SERVICE_PAUSED")
            .put("detail","Ekran hizmeti durmuş; ses oturumunda Telefon araçlarına izin ver")
        if (consentWindow!=null) return org.json.JSONObject().put("ok",false)
            .put("blocker","CONSENT_PENDING")
            .put("detail","PEGASUS ekran izin penceresine yanıt ver, ardından tekrar sor")
        val root=foregroundApplicationRoot() ?:''')

# First fix reading; then align scan/click/scroll with the same window source.
rep(a,'''        val root = rootInActiveWindow ?: run { updateText("Erişilebilir ekran yok") ; return }''',
'''        val root = foregroundApplicationRoot() ?: run {
            updateText("Android erişilebilir uygulama penceresi sunmadı"); return
        }''')
rep(a,'''        val root = rootInActiveWindow ?: run { updateText("Ekran değişmiş · Yeniden tara") ; return }''',
'''        val root = foregroundApplicationRoot() ?: run { updateText("Ekran değişmiş · Yeniden tara"); return }''')
rep(a,'''        val root = rootInActiveWindow ?: run { updateText("Ekran bulunamadı") ; return }''',
'''        val root = foregroundApplicationRoot() ?: run { updateText("Ekran bulunamadı"); return }''')

# Use focused actual app window for all package guards.
rep(a,'''    private fun safeGeminiScreen(): Boolean {
        val pkg=rootInActiveWindow?.packageName?.toString().orEmpty()
        return hasScreenAccess(pkg)
    }''',
'''    private fun safeGeminiScreen(): Boolean {
        val pkg=foregroundApplicationRoot()?.packageName?.toString().orEmpty()
        return hasScreenAccess(pkg)
    }''')
rep(a,'''        if (t.pkg != rootInActiveWindow?.packageName?.toString()) return null''',
'''        if (t.pkg != foregroundApplicationRoot()?.packageName?.toString()) return null''')
rep(a,'''            val pkg=rootInActiveWindow?.packageName?.toString().orEmpty()
            return blockedReason(pkg)''',
'''            val pkg=foregroundApplicationRoot()?.packageName?.toString().orEmpty()
            return blockedReason(pkg)''')

# Voice tools can ask native state. This does not reveal screen text.
rep(g,'''            "pegasus_read_screen" to "Kullanıcı 'ekranda ne yazıyor', ''',
'''            "pegasus_screen_status" to "Kullanıcı 'ekrana erişemiyorum', 'neden göremiyorsun' diye sorarsa önce bu aracı çağır. Erişilebilirlik ve izin durumunu bildirir, ekran metni göndermez.",
            "pegasus_read_screen" to "Kullanıcı 'ekranda ne yazıyor', ''')

# Make Android accessibility diagnosis available even if tools were not yet
# enabled so the assistant can tell the user exactly which setting is missing.
rep(v,'''    fun connected(): Boolean = live?.connected() == true''',
'''    fun toolsPermitted(): Boolean = running && toolsAllowed

    fun connected(): Boolean = live?.connected() == true''')
rep(v,'''        if (!toolsAllowed) { reply(result(false,
            "Bu oturumda telefon araçlarına izin verilmedi")); return }
        val access=PegasusAccessService.active''',
'''        if (name == "pegasus_screen_status") {
            if (!toolsAllowed) {
                reply(result(false,"Bu ses oturumunda Telefon araçlarına izin ver kapalı")
                    .put("code","TOOLS_OFF"))
            } else {
                reply(PegasusAccessService.active?.screenAccessDiagnostics()
                    ?: result(false,"Android erişilebilirlik hizmeti bağlı değil")
                        .put("code","A11Y_OFF"))
            }
            return
        }
        if (!toolsAllowed) { reply(result(false,
            "Bu oturumda telefon araçlarına izin verilmedi")
            .put("code","TOOLS_OFF")); return }
        val access=PegasusAccessService.active''')
rep(v,'''                      result(false,"Android erişilebilirlik hizmeti bağlı değil"))''',
'''                      result(false,"Android erişilebilirlik hizmeti bağlı değil")
                        .put("code","A11Y_OFF"))''')

# Add an explicit local diagnostic in existing NEXUS settings, no redesign.
rep(m,'''        @JavascriptInterface
        fun setActivityOrbEnabled(enabled: Boolean) {''',
'''        @JavascriptInterface
        fun checkScreenAccess() {
            runOnUiThread {
                val service=PegasusAccessService.active
                val voice=PegasusLiveService.instance
                val tools=voice?.toolsPermitted() ?: false
                val description=when {
                    service==null -> "Android Erişilebilirlik hizmeti PEGASUS'a bağlı değil. " +
                        "Hologram izni ekran okumaya yetmez."
                    voice==null -> "Gemini Live ses oturumu kapalı. Önce Sürekli ses oturumunu başlat."
                    !tools -> "Telefon araçları bu ses oturumunda kapalı. " +
                        "PEGASUS içinde Telefon araçlarına izin ver seçeneğini aç."
                    else -> service.screenAccessDiagnostics().optString("detail",
                        "Ekran durumu belirlenemedi")
                }
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("PEGASUS · Ekran erişimi tanısı")
                    .setMessage(description +
                        "\\n\\nİzinler farklıdır: 3B P hologramı için Ekran üstünde göster, " +
                        "metin okuma için Erişilebilirlik gerekir. " +
                        "Şifre, banka, mesaj, sistem izin ekranları korunur.")
                    .setNegativeButton("Kapat",null)
                    .setPositiveButton("Erişilebilirlik ayarları") { _, _ ->
                        try {
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        } catch (_: Exception) {
                            liveStatus("Android erişilebilirlik ayarları açılamadı")
                        }
                    }
                    .show()
            }
        }

        @JavascriptInterface
        fun setActivityOrbEnabled(enabled: Boolean) {''')
rep(html,'''  <p id="gemini-session-status" class="helper"''',
'''  <div class="action-row" style="margin-top:8px">
    <button class="btn ghost" id="screen-access-check" style="flex:1">Ekran erişimini kontrol et</button>
  </div>
  <p id="gemini-session-status" class="helper"''')
rep(html,'''document.querySelector('#phone-tools-disable').addEventListener('click',()=>{''',
'''document.querySelector('#screen-access-check').addEventListener('click',()=>{
  if(window.PegasusNative)window.PegasusNative.checkScreenAccess();
});
document.querySelector('#phone-tools-disable').addEventListener('click',()=>{''')

gradle=Path("app/build.gradle.kts")
rep(gradle,"versionCode = 510","versionCode = 511")
rep(gradle,'versionName = "1.0.10-system-alert-hologram"',
    'versionName = "1.0.11-screen-access-diagnostics"')
assert 'applicationId = "ai.pegasus.nexus.stable"' in gradle.read_text()
assert 'signingConfigs.getByName("pegasusStable")' in gradle.read_text()
assert 'SYSTEM_ALERT_WINDOW' in Path("app/src/main/AndroidManifest.xml").read_text()
assert 'TYPE_APPLICATION_OVERLAY' in v.read_text()
assert 'foregroundApplicationRoot()' in a.read_text()
assert 'CONSENT_PENDING' in a.read_text()
assert 'screenAccessDiagnostics' in a.read_text()
assert 'checkScreenAccess' in m.read_text()
assert 'pegasus_screen_status' in g.read_text()
print("PEGASUS_1_0_11_ACCESSIBILITY_ROOT_AND_DIAGNOSTICS=PASS")
