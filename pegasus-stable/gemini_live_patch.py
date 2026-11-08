#!/usr/bin/env python3
"""PEGASUS 1.0.2: optional Gemini Live session with short-lived token only.
Executed AFTER the existing 1.0.1 voice patch; does not change package or signing.
Network is used exclusively when a user explicitly starts a Gemini session.
Session closes whenever Android Activity leaves the foreground.
"""
from pathlib import Path
def rep(path, anchor, new, expected=1):
    f = Path(path); s=f.read_text(encoding="utf8")
    n=s.count(anchor)
    if n != expected: raise RuntimeError(f"{path}: expected {expected} occurrences, got {n}: {anchor[:90]}")
    f.write_text(s.replace(anchor,new,expected),encoding="utf8")

src=Path("app/src/main/java/ai/pegasus/nexus")
src.joinpath("PegasusGeminiLive.kt").write_text(r'''package ai.pegasus.nexus

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Base64
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Short-lived session token only. No API key is stored in APK, WebView storage or logs.
 * Explicitly foreground-only microphone; lifecycle closes the connection in onPause().
 * No model-generated phone actions, and no screen capture or accessibility-tree uploads.
 */
class PegasusGeminiLive(
    private val activity: Activity,
    private val onStatus: (String) -> Unit
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()
    private val main = Handler(Looper.getMainLooper())
    private val playbackQueue = LinkedBlockingQueue<ByteArray>(24)
    private var ws: WebSocket? = null
    private var record: AudioRecord? = null
    private var track: AudioTrack? = null
    private var micThread: Thread? = null
    private var playThread: Thread? = null
    @Volatile private var active = false
    @Volatile private var streaming = false
    @Volatile private var ready = false
    private var generation = 0

    private fun status(s: String) { main.post { onStatus(s) } }
    fun connected(): Boolean = ready && active

    fun start(ephemeralToken: String) {
        if (active) { status("Gemini oturumu zaten açık · önce kapat"); return }
        if (activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            status("Önce mikrofon izni ver"); return
        }
        if (ephemeralToken.length !in 20..1000 || !ephemeralToken.startsWith("auth_tokens/")) {
            status("Geçerli kısa ömürlü Gemini oturum anahtarı gerekli"); return
        }
        active = true
        ready = false
        val currentGeneration = ++generation
        status("Gemini Live'a güvenli bağlantı kuruluyor…")
        try {
            val endpoint = "wss://generativelanguage.googleapis.com/ws/" +
                "google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContentConstrained" +
                "?access_token=" + URLEncoder.encode(ephemeralToken, "UTF-8")
            val request = Request.Builder().url(endpoint).build()
            ws = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    if (!active || generation != currentGeneration) { webSocket.close(1000, "cancel"); return }
                    val setup = JSONObject().put("setup",
                        JSONObject().put("model", "models/gemini-3.8-live")
                            .put("responseModalities", JSONArray().put("AUDIO"))
                            .put("systemInstruction", JSONObject().put("parts",
                                JSONArray().put(JSONObject().put("text",
                                    "Sen PEGASUS'sun. Türkçe, doğal, kısa ve yardımsever cevap ver. " +
                                    "Telefon eylemlerini doğrudan gerçekleştiremezsin; " +
                                    "yaptığını iddia etme.")))))
                    webSocket.send(setup.toString())
                    status("Bağlantı açıldı · oturum hazırlanıyor")
                }
                override fun onMessage(webSocket: WebSocket, text: String) {
                    if (!active || generation != currentGeneration) return
                    try {
                        val m = JSONObject(text)
                        if (m.has("setupComplete") && !ready) {
                            ready = true
                            status("Gemini Live bağlandı · konuşabilirsin")
                            main.post { if (active && generation == currentGeneration) startAudio() }
                        }
                        val content = m.optJSONObject("serverContent")
                        if (content?.optBoolean("interrupted", false) == true) playbackQueue.clear()
                        val parts = content?.optJSONObject("modelTurn")?.optJSONArray("parts")
                        if (parts != null) for (i in 0 until parts.length()) {
                            val chunk = parts.optJSONObject(i)?.optJSONObject("inlineData") ?: continue
                            val data = chunk.optString("data")
                            val mime = chunk.optString("mimeType", "")
                            if (data.isNotEmpty() && mime.startsWith("audio/pcm")) {
                                val bytes = Base64.decode(data, Base64.DEFAULT)
                                playbackQueue.offer(bytes)
                            }
                        }
                        if (m.has("goAway")) { status("Oturum sonlanıyor · yeniden bağlan"); stop() }
                    } catch (_: Exception) {
                        status("Ses yanıtı işlenemedi")
                    }
                }
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    if (active && generation == currentGeneration) {
                        status("Gemini bağlantı hatası" + (response?.code?.let { " (HTTP $it)" } ?: "") +
                            " · anahtar, bağlantı veya model erişimini kontrol et")
                        main.post { stop() }
                    }
                }
                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    if (active && generation == currentGeneration) {
                        status("Gemini Live bağlantısı kapandı ($code)")
                        main.post { stop() }
                    }
                }
            })
        } catch (_: Exception) { status("Gemini oturumu başlatılamadı"); stop() }
    }

    private fun startAudio() {
        if (!active || !ready || streaming) return
        val minRec = AudioRecord.getMinBufferSize(16000,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val minPlay = AudioTrack.getMinBufferSize(24000,
            AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minRec <= 0 || minPlay <= 0) { status("Telefonun ses biçimi desteklenmiyor"); stop(); return }
        try {
            val input = AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, 16000,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minRec * 2, 8192))
            if (input.state != AudioRecord.STATE_INITIALIZED) {
                input.release(); status("Mikrofon açılamadı"); stop(); return
            }
            val output = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                .setAudioFormat(AudioFormat.Builder()
                    .setSampleRate(24000)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(maxOf(minPlay * 2, 8192))
                .setTransferMode(AudioTrack.MODE_STREAM).build()
            if (output.state != AudioTrack.STATE_INITIALIZED) {
                output.release(); input.release(); status("Ses çıkışı açılamadı"); stop(); return
            }
            record = input
            track = output
            streaming = true
            input.startRecording()
            output.play()
            playThread = Thread({
                while (streaming) {
                    try {
                        val chunk = playbackQueue.poll(150, TimeUnit.MILLISECONDS) ?: continue
                        val player = track ?: continue
                        player.write(chunk, 0, chunk.size)
                    } catch (_: Exception) { break }
                }
            }, "pegasus-gemini-speaker").also { it.isDaemon = true; it.start() }
            micThread = Thread({
                val chunk = ByteArray(3200) // 100 ms, PCM16 16 kHz mono
                while (streaming && active) {
                    val n = try { input.read(chunk, 0, chunk.size) } catch (_: Exception) { break }
                    if (n <= 0) continue
                    val audio = JSONObject().put("realtimeInput", JSONObject().put("audio",
                        JSONObject().put("data", Base64.encodeToString(chunk, 0, n, Base64.NO_WRAP))
                            .put("mimeType", "audio/pcm;rate=16000")))
                    if (ws?.send(audio.toString()) != true) break
                }
            }, "pegasus-gemini-mic").also { it.isDaemon = true; it.start() }
            status("Gemini Live · mikrofon aktif (DUR ile kapat)")
        } catch (_: SecurityException) { status("Mikrofon izni yok"); stop() }
          catch (_: Exception) { status("Mikrofon veya hoparlör başlatılamadı"); stop() }
    }

    fun stop() {
        active = false; ready = false; streaming = false; ++generation
        playbackQueue.clear()
        val oldRecord=record; record=null
        val oldTrack=track; track=null
        try { oldRecord?.stop() } catch (_: Exception) {}
        try { oldRecord?.release() } catch (_: Exception) {}
        try { oldTrack?.pause() } catch (_: Exception) {}
        try { oldTrack?.flush() } catch (_: Exception) {}
        try { oldTrack?.release() } catch (_: Exception) {}
        try { ws?.close(1000, "user stop") } catch (_: Exception) {}
        ws=null
        status("Gemini Live bağlı değil · mikrofon kapalı")
    }

    fun shutdown() { stop(); client.dispatcher.executorService.shutdown() }
}
''', encoding="utf8")

rep("app/build.gradle.kts", "versionCode = 501", "versionCode = 502")
rep("app/build.gradle.kts", 'versionName = "1.0.1-voice-test"',
    'versionName = "1.0.2-live-session-test"')
rep("app/build.gradle.kts", "    buildTypes {",
    '    dependencies { implementation("com.squareup.okhttp3:okhttp:4.12.0") }\n    buildTypes {',
    expected=0) if False else None
# dependencies { } belongs at project level, not within android.
f=Path("app/build.gradle.kts"); f.write_text(f.read_text()+"\n\ndependencies { implementation(\"com.squareup.okhttp3:okhttp:4.12.0\") }\n")

rep("app/src/main/AndroidManifest.xml",
    '    <uses-permission android:name="android.permission.RECORD_AUDIO" />',
    '    <uses-permission android:name="android.permission.RECORD_AUDIO" />\n    <uses-permission android:name="android.permission.INTERNET" />')

activity=src/"MainActivity.kt"
rep(activity,"import android.os.Bundle",
    "import android.os.Bundle\nimport org.json.JSONObject")
rep(activity,"    private lateinit var web: WebView",
    """    private lateinit var web: WebView
    private lateinit var live: PegasusGeminiLive
    private fun liveStatus(message: String) {
        if (!::web.isInitialized) return
        web.evaluateJavascript("window.pegasusGeminiStatus && window.pegasusGeminiStatus(" +
            JSONObject.quote(message) + ")", null)
    }""")
rep(activity,"        web = WebView(this).apply {",
    """        live = PegasusGeminiLive(this) { liveStatus(it) }
        web = WebView(this).apply {""")
rep(activity,"    private inner class NativeBridge {",
    """    override fun onPause() {
        // Recording is strictly foreground-only in this test release.
        if (::live.isInitialized) live.stop()
        super.onPause()
    }

    private inner class NativeBridge {
        @JavascriptInterface
        fun startGemini(token: String) {
            runOnUiThread { live.start(token.trim()) }
        }
        @JavascriptInterface
        fun stopGemini() {
            runOnUiThread { live.stop() }
        }
        @JavascriptInterface
        fun isGeminiConnected(): Boolean = live.connected()
""")
rep(activity,'        web.removeJavascriptInterface("PegasusNative")',
    '        live.shutdown()\n        web.removeJavascriptInterface("PegasusNative")')

html=Path("app/src/main/assets/nexus.html")
anchor='<section class="page" id="page-settings" aria-label="Ayarlar">'
panel='''<section class="page" id="page-settings" aria-label="Ayarlar">
<div class="panel" id="gemini-test-panel">
  <div class="cap">GEMINI LIVE · DENEME BAĞLANTISI</div>
  <p class="subheading">Güvenlik için yalnızca kısa ömürlü bir oturum anahtarı kabul edilir. Google API anahtarını buraya yazma. Oturum sadece PEGASUS ekrandayken açıktır; başka uygulamaya geçtiğinde mikrofon kapanır.</p>
  <input class="input" type="password" id="gemini-ephemeral" autocomplete="off" placeholder="auth_tokens/... geçici anahtar">
  <div class="action-row" style="margin-top:9px">
    <button class="btn primary" id="gemini-connect" style="flex:1">Gemini'ye bağlan</button>
    <button class="btn ghost" id="gemini-disconnect" style="flex:1">DUR · Kapat</button>
  </div>
  <p id="gemini-session-status" class="helper" style="margin-top:8px">Gemini Live bağlı değil · geçici anahtar gerekli</p>
</div>'''
rep(html,anchor,panel)
# Add event listeners after IIFE begins, so connection status is visible in both sections.
js='''window.pegasusGeminiStatus = s => {
  const el=document.querySelector('#gemini-session-status');
  if (el) el.textContent=String(s);
  const home=document.querySelector('#home-gemini-status');
  if(home)home.textContent=String(s);
};
document.querySelector('#gemini-connect').addEventListener('click',()=>{
  const input=document.querySelector('#gemini-ephemeral');
  const key=input.value.trim(); input.value='';
  if(window.PegasusNative)window.PegasusNative.startGemini(key);
  else window.pegasusGeminiStatus('Bu işlev yalnızca Android uygulamasında çalışır');
});
document.querySelector('#gemini-disconnect').addEventListener('click',()=>{
  if(window.PegasusNative)window.PegasusNative.stopGemini();
  window.pegasusGeminiStatus('Gemini Live bağlı değil · mikrofon kapalı');
});
'''
rep(html, "function modal(title,body,actions){",js+"function modal(title,body,actions){")
rep(html, '<b>Gemini Live</b><span>Bağlı değil</span>',
    '<b>Gemini Live</b><span id="home-gemini-status">Bağlı değil</span>')
rep(html, 'Gemini Live henüz bağlı değil. Aşağıda sohbet tasarımı ve yerel taslak alanı var.',
    'Gemini Live isteğe bağlı sesli oturum: Ayarlar bölümünden geçici anahtarla bağlan. Sohbet metni burada hâlâ yerel taslaktır.')

# Static gates: no secret, permission regression, no package identity change.
gradle=Path("app/build.gradle.kts").read_text()
manifest=Path("app/src/main/AndroidManifest.xml").read_text()
live=(src/"PegasusGeminiLive.kt").read_text()
assert 'applicationId = "ai.pegasus.nexus.stable"' in gradle
assert 'versionCode = 502' in gradle
assert 'signingConfigs.getByName("pegasusStable")' in gradle
assert "android.permission.INTERNET" in manifest and "android.permission.RECORD_AUDIO" in manifest
assert "BidiGenerateContentConstrained" in live
assert "onPause()" in activity.read_text()
print("PEGASUS_1_0_2_GEMINI_EPHEMERAL_SESSION_PATCH=PASS")
