#!/usr/bin/env python3
"""PEGASUS Stable 1.0.8: distinguish Android restrictions from PEGASUS
own app allowlist and request per-app, per-voice-session explicit consent.

No OS security bypass, no permission automation, no silent or automatic
screen capture. Existing NEXUS look/Live session remains unchanged.
"""
from pathlib import Path

def rep(path, a, b):
    p=Path(path); s=p.read_text(encoding="utf-8")
    n=s.count(a)
    if n != 1: raise RuntimeError(f"Patch count {n}, expected 1 in {p}: {a[:120]}")
    p.write_text(s.replace(a,b),encoding="utf-8")

src=Path("app/src/main/java/ai/pegasus/nexus")
service=src/"PegasusAccessService.kt"
live=src/"PegasusGeminiLive.kt"
activity=src/"MainActivity.kt"
html=Path("app/src/main/assets/nexus.html")

# A separate native consent window, shown ONLY after the user asks Gemini to
# read or scan an otherwise unsupported regular app. It is not the floating P.
rep(service, '''    fun setOverlayVisible(visible: Boolean) {''',
'''    // Consent applies to only one running voice-tools session. We never
    // persist package grants or override Android's protected windows.
    private val temporaryReadGrants = mutableSetOf<String>()
    private var consentWindow: android.view.View? = null
    private var consentPackage: String? = null

    fun clearScreenReadGrants() {
        temporaryReadGrants.clear()
        consentPackage = null
        val old=consentWindow
        consentWindow=null
        if (old != null) {
            try {
                (getSystemService(WINDOW_SERVICE) as android.view.WindowManager)
                    .removeView(old)
            } catch (_: Exception) {}
        }
    }

    private fun policyAppBlock(pkg: String): String? {
        val lower=pkg.lowercase(java.util.Locale.ROOT)
        if (pkg.isBlank()) return "Android aktif pencereyi bildirmiyor"
        if (ActionPolicy.isRestrictedPackage(pkg)) return "PEGASUS korunan uygulama politikası"
        val protectedNames=listOf(
            "systemui","permissioncontroller","settings","keyguard",
            "bank","banking","finance","wallet","payment","paycell",
            "cryptocurrency","authenticator","password","vault","credential",
            "com.whatsapp","org.telegram","signal","discord",
            "com.google.android.apps.messaging","mms","dialer",
            "com.android.vending","com.google.android.gms",
            "com.android.chrome","org.mozilla.firefox")
        if (protectedNames.any { lower.contains(it) })
            return "Bu uygulama hesap, mesaj, kimlik veya ödeme içeriği barındırabilir"
        return null
    }

    private fun basicReadAllowed(pkg: String) = pkg in setOf(
        "ai.pegasus.nexus.stable","com.android.calculator2",
        "com.google.android.calculator","com.miui.calculator","org.wikipedia"
    )

    private fun hasScreenAccess(pkg: String): Boolean =
        policyAppBlock(pkg) == null &&
            (basicReadAllowed(pkg) || temporaryReadGrants.contains(pkg))

    private fun requestScreenConsent(pkg: String) {
        if (policyAppBlock(pkg)!=null || consentWindow!=null || consentPackage==pkg) return
        consentPackage=pkg
        main.post {
            if (consentWindow!=null) return@post
            if (rootInActiveWindow?.packageName?.toString()!=pkg) {
                consentPackage=null
                return@post
            }
            val wm=getSystemService(WINDOW_SERVICE) as android.view.WindowManager
            val d=resources.displayMetrics.density
            fun dip(n:Int)=(n*d).toInt()
            val box=android.widget.LinearLayout(this).apply {
                orientation=android.widget.LinearLayout.VERTICAL
                setPadding(dip(18),dip(16),dip(18),dip(14))
                setBackgroundColor(android.graphics.Color.rgb(25,25,25))
                elevation=dip(8).toFloat()
            }
            val label=android.widget.TextView(this).apply {
                text="PEGASUS · Ekran okuma izni\\n\\n" +
                    "Uygulama: "+pkg.take(95)+"\\n\\n" +
                    "İzin verirsen bu oturumda yalnızca bu uygulamanın " +
                    "görünür ve filtrelenen erişilebilir metni, sen istediğinde Gemini'ye " +
                    "aktarılabilir. Şifre, ödeme ve korumalı ekranlar hariçtir. " +
                    "DUR ile izin kapanır."
                setTextColor(android.graphics.Color.WHITE)
                textSize=15f
            }
            box.addView(label)
            val actions=android.widget.LinearLayout(this).apply {
                orientation=android.widget.LinearLayout.HORIZONTAL
            }
            fun close(accepted:Boolean) {
                if (accepted && rootInActiveWindow?.packageName?.toString()==pkg &&
                    policyAppBlock(pkg)==null) {
                    temporaryReadGrants.add(pkg)
                    updateText("Bu oturumda "+pkg.take(40)+" ekran okumaya izinli · komutu tekrar söyle")
                } else updateText("Ekran okumaya izin verilmedi")
                val v=consentWindow
                consentWindow=null
                consentPackage=null
                if(v!=null) try { wm.removeView(v) } catch (_: Exception) {}
            }
            val decline=android.widget.Button(this).apply {
                text="Reddet"; setOnClickListener { close(false) }
            }
            val grant=android.widget.Button(this).apply {
                text="Bu oturumda izin ver"; setOnClickListener { close(true) }
            }
            actions.addView(decline)
            actions.addView(grant)
            box.addView(actions)
            val params=android.view.WindowManager.LayoutParams(
                dip(340),android.view.WindowManager.LayoutParams.WRAP_CONTENT,
                android.view.WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                android.graphics.PixelFormat.TRANSLUCENT
            ).apply {
                gravity=android.view.Gravity.CENTER
            }
            try {
                wm.addView(box,params)
                consentWindow=box
            } catch (_: Exception) {
                consentWindow=null
                consentPackage=null
                updateText("Android izin penceresi açılamadı · erişilebilirlik hizmetini kontrol et")
            }
        }
    }

    private fun blockedReason(pkg:String): org.json.JSONObject {
        val reason=policyAppBlock(pkg)
        if (reason!=null)
            return org.json.JSONObject().put("ok",false)
                .put("blocker","PEGASUS_PROTECTION")
                .put("detail", reason+" · sistem güvenlik sınırları aşılmaz")
        requestScreenConsent(pkg)
        return org.json.JSONObject().put("ok",false)
            .put("blocker","PEGASUS_CONSENT")
            .put("detail","Bu uygulama PEGASUS'un eski izin listesinde yok. " +
                "Ekrandaki bir defalık izni kabul edip komutu yeniden söyle")
    }

    fun setOverlayVisible(visible: Boolean) {''')

# Restrict to protected apps, or allow explicitly approved third-party apps.
rep(service, '''        if (ActionPolicy.isRestrictedPackage(pkg)) return fail("Korunan uygulama · ekran okuma engellendi")
        // Explicit allowlist. No messaging, bank, passwords, browser sign-in
        // pages or system settings are read in this initial build.
        if (pkg !in setOf("ai.pegasus.nexus.stable",
                         "com.android.calculator2",
                         "com.google.android.calculator",
                         "com.miui.calculator",
                         "org.wikipedia")) {
            return fail("Bu uygulamada metin aktarımı henüz açık değil; gizliliğin için engellendi")
        }''',
'''        if (!hasScreenAccess(pkg)) return blockedReason(pkg)
        // A regular app can be allowed for this single session by native
        // overlay confirmation. Sensitive apps are still always blocked.''')

rep(service,'''        val root=rootInActiveWindow ?: return fail("Görünür erişilebilir metin bulunamadı")''',
'''        val root=rootInActiveWindow ?:
            return org.json.JSONObject().put("ok",false)
                .put("blocker","ANDROID_NO_WINDOW")
                .put("detail","Android aktif pencerenin erişilebilir içeriğini sunmadı; " +
                    "erişilebilirlik ayarını ve uygulamanın ekranını kontrol et")''')

rep(service, '''        if (text.isBlank()) return fail("Erişilebilir metin yok · görsel/OCR desteği henüz eklenmedi")''',
'''        if (text.isBlank()) return org.json.JSONObject().put("ok",false)
            .put("blocker","NO_ACCESSIBLE_TEXT")
            .put("detail","Bu ekranda erişilebilir yazı yok. Video, çizim veya görüntü " +
                "içindeki yazılar için ayrı görsel/OCR özelliği gerekiyor")''')

rep(service,'''    private fun safeGeminiScreen(): Boolean {
        val pkg = rootInActiveWindow?.packageName?.toString().orEmpty()
        return pkg == "ai.pegasus.nexus.stable" ||
            pkg == "com.android.calculator2" ||
            pkg == "com.google.android.calculator" ||
            pkg == "com.miui.calculator"
    }''',
'''    private fun safeGeminiScreen(): Boolean {
        val pkg=rootInActiveWindow?.packageName?.toString().orEmpty()
        return hasScreenAccess(pkg)
    }''')

rep(service, '''        if (!safeGeminiScreen()) {
            return result(false, "Bu uygulama 1.0.6 güvenli araç testinde desteklenmiyor")
        }''',
'''        if (!safeGeminiScreen()) {
            val pkg=rootInActiveWindow?.packageName?.toString().orEmpty()
            return blockedReason(pkg)
        }''')

rep(service, '''        if (stopped) return result(false, "Ekran kontrolü durdu; P panelinden Başlat gerekir")''',
'''        if (stopped) return result(false, "Ekran kontrolü durdu; ses oturumunda telefon araçlarına yeniden izin ver")''')

# On leaving screen-session or explicit stop the per-app read grants disappear.
rep(service, '''    fun emergencyStop() {
        voiceListener?.stop()
        stopped = true''',
'''    fun emergencyStop() {
        voiceListener?.stop()
        clearScreenReadGrants()
        stopped = true''')

# model should acknowledge why denied and ask explicit consent only once
rep(live,'''                                    "Ekran verisi gelmeden gördüğünü iddia etme. " +''',
'''                                    "Ekran verisi gelmeden gördüğünü iddia etme. " +
                                    "Araç sonucu blocker=PEGASUS_CONSENT ise kullanıcının " +
                                    "telefonunda çıkan tek seferlik izin ekranını açıkla. " +
                                    "blocker=PEGASUS_PROTECTION veya ANDROID_NO_WINDOW ise " +
                                    "güvenlik korumasını aşmaya çalışma. " +''')

# UI truth-in-advertising; semantic reading isn't screenshot/OCR.
rep(html,'''Ekran okuma yalnızca desteklenen, hassas olmayan uygulamalarda çalışır.''',
'''Normal uygulamalarda ilk seferde tek oturumluk metin okuma onayı istenebilir. " +
    "Şifre, banka, mesaj, sistem izin ekranları korunur; görsel OCR henüz yoktur.''') if False else None

gradle=Path("app/build.gradle.kts")
rep(gradle,"versionCode = 507","versionCode = 508")
rep(gradle,'versionName = "1.0.7-background-semantic-test"',
    'versionName = "1.0.8-screen-consent-diagnostics"')

g=gradle.read_text()
a=service.read_text()
l=live.read_text()
assert 'applicationId = "ai.pegasus.nexus.stable"' in g
assert 'signingConfigs.getByName("pegasusStable")' in g
assert "versionCode = 508" in g
assert "PEGASUS_CONSENT" in a and "ANDROID_NO_WINDOW" in a
assert "TYPE_ACCESSIBILITY_OVERLAY" in a and "clearScreenReadGrants()" in a
assert "NO_ACCESSIBLE_TEXT" in a and "PEGASUS_PROTECTION" in a
assert "blocker=PEGASUS_CONSENT" in l
print("PEGASUS_SCREEN_CONSENT_DIAGNOSTICS_1_0_8_PATCH=PASS")
