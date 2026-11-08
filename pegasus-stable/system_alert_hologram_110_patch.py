#!/usr/bin/env python3
"""PEGASUS Stable 1.0.10: real SYSTEM_ALERT_WINDOW P hologram.
Keeps the original NEXUS design and all Gemini tools exactly as in 1.0.9.
The visual orb is a passive TYPE_APPLICATION_OVERLAY that exists only while
the user-started microphone foreground service is running, and only after
an explicit user-granted Android 'Display over other apps' permission.
"""
from pathlib import Path

def rep(path, old, new):
    p=Path(path)
    s=p.read_text(encoding="utf-8")
    count=s.count(old)
    if count != 1:
        raise RuntimeError(f"{p} expected exactly one anchor, found {count}: {old[:120]}")
    p.write_text(s.replace(old,new), encoding="utf-8")

src=Path("app/src/main/java/ai/pegasus/nexus")
service=src/"PegasusLiveService.kt"
access=src/"PegasusAccessService.kt"
activity=src/"MainActivity.kt"
html=Path("app/src/main/assets/nexus.html")
manifest=Path("app/src/main/AndroidManifest.xml")
orb=Path("app/src/main/assets/pegasus_activity_orb.html")
assert orb.exists(), "1.0.9 PEGASUS P hologram missing"

# Remove previous 1.0.9 accessibility-overlay implementation altogether.
# The 1.0.10 orb is independently rendered by the microphone FGS, so it
# appears regardless of the accessibility permission or the small P panel.
a=access.read_text(encoding="utf-8")
begin=a.index("    private var activityOrb: android.webkit.WebView? = null")
end=a.index("    fun setOverlayVisible(visible: Boolean) {",begin)
a=a[:begin]+a[end:]
access.write_text(a,encoding="utf-8")
assert "TYPE_ACCESSIBILITY_OVERLAY" in a  # Existing control/consent overlays only
assert "activityOrb" not in a

# Android's special permission is NOT auto-granted by a manifest line.
# Android settings screen must be opened by the user via a real click.
rep(manifest,
'''    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />''',
'''    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />''')

# Hologram lives on the foreground service (which survives Activity.onPause).
# No controls, hidden web bridges, click interception or scanning. Pass-through
# touch flags protect other apps. If OS refuses permission, audio still works.
rep(service,'''    private fun say(message:String) {''',
'''    private var hologram: android.webkit.WebView? = null
    private var hologramState = "off"
    private var hologramWindow: android.view.WindowManager? = null
    private val hologramPrefs by lazy {
        getSharedPreferences("pegasus_activity_orb", MODE_PRIVATE)
    }

    /** Called after explicit settings consent or when returning from Android
     * special app access. Never launches an activity or grants permissions.
     */
    fun refreshHologramPermission() {
        handler.post { renderHologram(hologramState) }
    }

    fun setHologramEnabled(enabled: Boolean) {
        hologramPrefs.edit().putBoolean("enabled", enabled).apply()
        handler.post { renderHologram(hologramState) }
    }

    /** Visual state is derived solely from the actual Live session. */
    private fun updateHologram(state: String) {
        hologramState=state
        handler.post { renderHologram(state) }
    }

    private fun renderHologram(state: String) {
        if (!running || state=="off" ||
            !hologramPrefs.getBoolean("enabled",true) ||
            !android.provider.Settings.canDrawOverlays(this)) {
            removeHologram()
            return
        }
        val old=hologram
        if (old!=null) {
            try {
                old.evaluateJavascript(
                    "window.setPegasusState("+
                    org.json.JSONObject.quote(state)+");",null)
            } catch (_: Exception) { removeHologram() }
            return
        }
        val d=resources.displayMetrics.density
        val v=android.webkit.WebView(this)
        v.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        v.settings.javaScriptEnabled=true
        v.settings.domStorageEnabled=false
        v.settings.allowFileAccess=true
        v.settings.allowContentAccess=false
        @Suppress("DEPRECATION")
        fun lockDownWebViewFileAccess() {
            v.settings.allowFileAccessFromFileURLs=false
            v.settings.allowUniversalAccessFromFileURLs=false
        }
        lockDownWebViewFileAccess()
        v.webViewClient=object:android.webkit.WebViewClient() {
            override fun onPageFinished(view:android.webkit.WebView?, url:String?) {
                if (view != null && view === hologram && url=="file:///android_asset/pegasus_activity_orb.html") {
                    view.evaluateJavascript(
                        "window.setPegasusState("+
                        org.json.JSONObject.quote(hologramState)+");",null)
                }
            }
        }
        val wm=getSystemService(WINDOW_SERVICE) as android.view.WindowManager
        val size=(92*d).toInt()
        val lp=android.view.WindowManager.LayoutParams(
            size,size,
            android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity=android.view.Gravity.TOP or android.view.Gravity.END
            x=(12*d).toInt()
            y=(115*d).toInt()
            // Android 12+ rejects touch pass-through if a third-party
            // TYPE_APPLICATION_OVERLAY is too opaque (default threshold 0.8).
            // This indicator is passive: keep the window safely translucent.
            alpha=0.75f
            setTitle("PEGASUS · Canlı P Hologram")
        }
        try {
            wm.addView(v,lp)
            hologramWindow=wm
            hologram=v
            v.loadUrl("file:///android_asset/pegasus_activity_orb.html")
        } catch (_: Exception) {
            try { wm.removeView(v) } catch (_: Exception) {}
            try { v.destroy() } catch (_: Exception) {}
            hologram=null
            hologramWindow=null
            // Never claim hologram is visible when Android denied the window.
        }
    }

    private fun removeHologram() {
        val view=hologram ?: return
        hologram=null
        try { hologramWindow?.removeView(view) } catch (_: Exception) {}
        hologramWindow=null
        try {
            view.stopLoading()
            view.loadUrl("about:blank")
            view.destroy()
        } catch (_: Exception) {}
    }

    private fun say(message:String) {''')

# All calls in version 1.0.9 to the accessibility overlay become FGS calls.
s=service.read_text(encoding="utf-8")
assert s.count('PegasusAccessService.active?.updateActivityOrb(state)')==1
assert s.count('PegasusAccessService.active?.updateActivityOrb("off")')==2
s=s.replace('PegasusAccessService.active?.updateActivityOrb(state)',
            'updateHologram(state)')
s=s.replace('PegasusAccessService.active?.updateActivityOrb("off")',
            'updateHologram("off")')
service.write_text(s,encoding="utf-8")

# Cleanup is synchronous in onDestroy, not just a delayed Handler callback.
rep(service,'''    override fun onDestroy() {
        running=false''',
'''    override fun onDestroy() {
        running=false
        removeHologram()''')

# If service stops after user hits notification DUR, immediately remove bubble.
rep(service,'''    private fun stopSession() {
        running=false''',
'''    private fun stopSession() {
        running=false
        removeHologram()''')

# Activity only controls visible permission UI and the service's persisted
# visual switch. It NEVER renders an always-on fake 'Gemini connected' state.
rep(activity,'''        @JavascriptInterface
        fun setActivityOrbEnabled(enabled: Boolean) {
            runOnUiThread {
                PegasusAccessService.active?.setActivityOrbEnabled(enabled)
                liveStatus(if(enabled) "PEGASUS çalışma simgesi açık"
                           else "PEGASUS çalışma simgesi gizli")
            }
        }''',
'''        @JavascriptInterface
        fun setActivityOrbEnabled(enabled: Boolean) {
            runOnUiThread {
                getSharedPreferences("pegasus_activity_orb", MODE_PRIVATE)
                    .edit().putBoolean("enabled",enabled).apply()
                PegasusLiveService.instance?.setHologramEnabled(enabled)
                if (enabled &&
                    !android.provider.Settings.canDrawOverlays(this@MainActivity)) {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("PEGASUS · 3B P hologram izni")
                        .setMessage(
                            "Hareketli P simgesinin diğer uygulamaların üzerinde " +
                            "görünmesi için Android'in 'Diğer uygulamaların üzerinde " +
                            "göster' iznini senin açman gerekiyor. " +
                            "Simge yalnızca Gemini Live ses oturumu çalışırken " +
                            "görünür; dokunmaları engellemez. DUR ile kaybolur. " +
                            "Mikrofon ve ekran okuma izinleri bundan ayrıdır."
                        )
                        .setNegativeButton("Şimdi değil",null)
                        .setPositiveButton("İzin ayarlarına git") { _, _ ->
                            try {
                                startActivity(Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION
                                ))
                            } catch (_: Exception) {
                                liveStatus("Hologram izni için Android uygulama ayarlarına git")
                            }
                        }
                        .show()
                } else liveStatus(
                    if(enabled) "P hologramı açık · Gemini çalışırken görünür"
                    else "P hologramı kapalı"
                )
            }
        }''')

# Android returns from its permission screen via onResume; render only if
# user explicitly granted it AND the foreground voice service is running.
rep(activity,'''        foregroundActivity=this
        if (::web.isInitialized) liveStatus(PegasusLiveService.lastStatus)''',
'''        foregroundActivity=this
        PegasusLiveService.instance?.refreshHologramPermission()
        if (::web.isInitialized) liveStatus(PegasusLiveService.lastStatus)''')

# The NEXUS theme is intentionally untouched. Change only the existing
# two settings button labels that version 1.0.9 already introduced.
rep(html,'''id="orb-enable" style="flex:1">Çalışma simgesini aç''',
'''id="orb-enable" style="flex:1">3B P hologramını aç''')
rep(html,'''id="orb-disable" style="flex:1">Çalışma simgesini gizle''',
'''id="orb-disable" style="flex:1">3B P hologramını gizle''')

gradle=Path("app/build.gradle.kts")
rep(gradle,"versionCode = 509","versionCode = 510")
rep(gradle,'versionName = "1.0.9-activity-orb"',
    'versionName = "1.0.10-system-alert-hologram"')

assert 'applicationId = "ai.pegasus.nexus.stable"' in gradle.read_text()
assert 'signingConfigs.getByName("pegasusStable")' in gradle.read_text()
assert 'SYSTEM_ALERT_WINDOW' in manifest.read_text()
assert 'TYPE_APPLICATION_OVERLAY' in service.read_text()
assert 'FLAG_NOT_TOUCHABLE' in service.read_text()
assert 'Settings.ACTION_MANAGE_OVERLAY_PERMISSION' in activity.read_text()
assert 'TYPE_ACCESSIBILITY_OVERLAY' not in service.read_text()
assert 'pegasus_activity_orb.html' in service.read_text()
assert 'id="orb-enable"' in html.read_text()
print("PEGASUS_SYSTEM_ALERT_WINDOW_P_HOLOGRAM_1_0_10_PATCH=PASS")
