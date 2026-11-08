#!/usr/bin/env python3
"""PEGASUS Stable 1.0.9: adapt user's supplied NEMESIS animated orbit
as a passive PEGASUS P AI activity indicator. No NEMESIS code or branding
in app, no wakeword/recording claims, no overlay required for control.
"""
from pathlib import Path
def rep(p,a,b):
    p=Path(p);s=p.read_text(encoding="utf-8");n=s.count(a)
    if n!=1: raise RuntimeError(f"{p}: expected 1 anchor, got {n}: {a[:100]}")
    p.write_text(s.replace(a,b),encoding="utf-8")

src=Path("app/src/main/java/ai/pegasus/nexus")
access=src/"PegasusAccessService.kt"
service=src/"PegasusLiveService.kt"
activity=src/"MainActivity.kt"
html=Path("app/src/main/assets/nexus.html")
orb=Path("app/src/main/assets/pegasus_activity_orb.html")
orb.write_text(r'''<!doctype html>
<html lang="tr"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1,user-scalable=no">
<style>
*{box-sizing:border-box}html,body{margin:0;width:100%;height:100%;background:transparent;overflow:hidden}
canvas{width:100%;height:100%;display:block}
</style></head><body><canvas id="orb" width="320" height="320"></canvas>
<script>
/* Reimplemented from the user's supplied NEMESIS canvas orbit design,
   PEGASUS P and black/white NEXUS styling; no fake status, no click handlers. */
const c=document.getElementById("orb"),ctx=c.getContext("2d");
let t=0,mode="off";
const colors={connected:"#fff",connecting:"#a0a0a0",speaking:"#fff",
              error:"#777",off:"#777"};
function ring(front,r){
 ctx.save();ctx.translate(160,160);ctx.rotate(-.24+t*.07);
 for(let k=0;k<4;k++){
   const rx=r*(1.30+k*.12),ry=r*(.39+(k%2)*.10);
   ctx.save();ctx.rotate((k-1.5)*.22);
   ctx.beginPath();let start=false;
   for(let i=0;i<=80;i++){
     const a=i/80*Math.PI*2;
     if((Math.sin(a)>=0)!==front){start=false;continue}
     const wobble=Math.sin(a*3+t*(1.5+k*.2))*4;
     const x=Math.cos(a)*(rx+wobble),y=Math.sin(a)*ry;
     if(!start){ctx.moveTo(x,y);start=true}else ctx.lineTo(x,y)
   }
   ctx.lineWidth=front?2.3:1.1;
   ctx.strokeStyle=front?"rgba(255,255,255,.85)":"rgba(255,255,255,.23)";
   ctx.stroke();ctx.restore()
 }
 ctx.restore()
}
function frame(){
 ctx.clearRect(0,0,320,320);
 t+=mode==="speaking"?.065:mode==="connecting"?.045:.025;
 const r=72+(mode==="speaking"?4*Math.sin(t*3):0);
 ring(false,r);
 const g=ctx.createRadialGradient(140,140,5,160,160,r);
 g.addColorStop(0,"#393939");g.addColorStop(.55,"#111114");g.addColorStop(1,"#030305");
 ctx.beginPath();ctx.arc(160,160,r,0,Math.PI*2);ctx.fillStyle=g;ctx.fill();
 ctx.lineWidth=2;ctx.strokeStyle=colors[mode]||"#777";ctx.stroke();
 ctx.save();ctx.translate(160,160);ctx.shadowColor="#fff";ctx.shadowBlur=mode==="off"?4:18;
 ctx.strokeStyle="#fff";ctx.lineWidth=7;ctx.lineJoin="round";ctx.lineCap="round";
 // Stylized P, not N. Retains the source's central-letter and orbit composition.
 ctx.beginPath();ctx.moveTo(-18,28);ctx.lineTo(-18,-27);ctx.lineTo(8,-27);
 ctx.bezierCurveTo(37,-27,37,4,8,5);ctx.lineTo(-18,5);ctx.stroke();ctx.restore();
 ring(true,r);
 requestAnimationFrame(frame)
}
window.setPegasusState=function(v){
 mode=["connected","connecting","speaking","error","off"].includes(v)?v:"off"
};
frame();
</script></body></html>
''',encoding="utf-8")

# Passive, non-touchable overlay; explicitly no P-panel dependency.
rep(access,'''    fun setOverlayVisible(visible: Boolean) {''',
'''    private var activityOrb: android.webkit.WebView? = null
    private var activityOrbState: String = "off"
    private val orbSettings by lazy {
        getSharedPreferences("pegasus_activity_orb", MODE_PRIVATE)
    }

    fun setActivityOrbEnabled(enabled: Boolean) {
        orbSettings.edit().putBoolean("enabled",enabled).apply()
        main.post {
            if (!enabled) removeActivityOrb()
            else updateActivityOrb(activityOrbState)
        }
    }

    fun isActivityOrbEnabled(): Boolean =
        orbSettings.getBoolean("enabled",true)

    fun updateActivityOrb(state:String) {
        activityOrbState=state
        main.post {
            if (!isActivityOrbEnabled() || state=="off") {
                removeActivityOrb()
                return@post
            }
            val current=activityOrb
            if(current==null) {
                val v=android.webkit.WebView(this)
                v.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                v.settings.javaScriptEnabled=true
                v.settings.allowFileAccess=true
                v.settings.domStorageEnabled=false
                val d=resources.displayMetrics.density
                val size=(92*d).toInt()
                val lp=android.view.WindowManager.LayoutParams(
                    size,size,
                    android.view.WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    android.graphics.PixelFormat.TRANSLUCENT
                ).apply {
                    gravity=android.view.Gravity.TOP or android.view.Gravity.END
                    x=(12*d).toInt()
                    y=(118*d).toInt()
                }
                try {
                    (getSystemService(WINDOW_SERVICE) as android.view.WindowManager)
                        .addView(v,lp)
                    activityOrb=v
                    v.loadUrl("file:///android_asset/pegasus_activity_orb.html")
                    v.webViewClient=object:android.webkit.WebViewClient(){
                        override fun onPageFinished(
                            view:android.webkit.WebView,url:String?) {
                            view.evaluateJavascript(
                                "window.setPegasusState("+
                                org.json.JSONObject.quote(activityOrbState)+");",null)
                        }
                    }
                } catch (_: Exception) { v.destroy(); activityOrb=null }
            } else {
                current.evaluateJavascript(
                    "window.setPegasusState("+
                    org.json.JSONObject.quote(state)+");",null)
            }
        }
    }

    private fun removeActivityOrb() {
        val v=activityOrb ?: return
        activityOrb=null
        try {
            (getSystemService(WINDOW_SERVICE) as android.view.WindowManager)
                .removeView(v)
        } catch (_: Exception) {}
        v.destroy()
    }

    fun setOverlayVisible(visible: Boolean) {''')

# Voice connection status drives orb; never claim "active" when disconnected.
rep(service,'''    private fun say(message:String) {
        lastStatus=message''',
'''    private fun say(message:String) {
        val lower=message.lowercase(java.util.Locale.ROOT)
        val state=when {
            !running -> "off"
            "hata" in lower || "kesildi" in lower || "başlatılamadı" in lower ->
                "error"
            "yenileniyor" in lower || "hazırlanıyor" in lower ||
                "kuruluyor" in lower -> "connecting"
            "mikrofon aktif" in lower || "bağlandı" in lower -> "connected"
            else -> if (live?.connected()==true) "connected" else "connecting"
        }
        PegasusAccessService.active?.updateActivityOrb(state)
        lastStatus=message''')
rep(service,'''        lastStatus="Gemini Live kapalı · mikrofon kapalı"
        MainActivity.foregroundActivity?.pushLiveStatus(lastStatus)''',
'''        lastStatus="Gemini Live kapalı · mikrofon kapalı"
        PegasusAccessService.active?.updateActivityOrb("off")
        MainActivity.foregroundActivity?.pushLiveStatus(lastStatus)''')
rep(service,'''        lastStatus="Gemini Live kapalı · mikrofon kapalı"
        super.onDestroy()''',
'''        lastStatus="Gemini Live kapalı · mikrofon kapalı"
        PegasusAccessService.active?.updateActivityOrb("off")
        super.onDestroy()''')

# Toggle from PEGASUS settings, independent of existing P-panel toggle.
rep(activity,'''        @JavascriptInterface
        fun setOverlayEnabled(enabled: Boolean) {''',
'''        @JavascriptInterface
        fun setActivityOrbEnabled(enabled: Boolean) {
            runOnUiThread {
                PegasusAccessService.active?.setActivityOrbEnabled(enabled)
                liveStatus(if(enabled) "PEGASUS çalışma simgesi açık"
                           else "PEGASUS çalışma simgesi gizli")
            }
        }

        @JavascriptInterface
        fun setOverlayEnabled(enabled: Boolean) {''')
rep(html,'''  <p id="gemini-session-status" class="helper"''',
'''  <div class="action-row" style="margin-top:8px">
    <button class="btn ghost" id="orb-enable" style="flex:1">Çalışma simgesini aç</button>
    <button class="btn ghost" id="orb-disable" style="flex:1">Çalışma simgesini gizle</button>
  </div>
  <p id="gemini-session-status" class="helper"''')
rep(html,'''document.querySelector('#overlay-hide').addEventListener('click',()=>{''',
'''document.querySelector('#orb-enable').addEventListener('click',()=>{
  if(window.PegasusNative) window.PegasusNative.setActivityOrbEnabled(true);
});
document.querySelector('#orb-disable').addEventListener('click',()=>{
  if(window.PegasusNative) window.PegasusNative.setActivityOrbEnabled(false);
});
document.querySelector('#overlay-hide').addEventListener('click',()=>{''')

gradle=Path("app/build.gradle.kts")
rep(gradle,"versionCode = 508","versionCode = 509")
rep(gradle,'versionName = "1.0.8-screen-consent-diagnostics"',
    'versionName = "1.0.9-activity-orb"')
assert 'applicationId = "ai.pegasus.nexus.stable"' in gradle.read_text()
assert 'signingConfigs.getByName("pegasusStable")' in gradle.read_text()
assert 'setPegasusState' in orb.read_text()
assert 'FLAG_NOT_TOUCHABLE' in access.read_text()
assert 'setActivityOrbEnabled' in activity.read_text()
print("PEGASUS_1_0_9_ACTIVITY_ORB_PATCH=PASS")
