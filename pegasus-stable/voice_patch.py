#!/usr/bin/env python3
"""PEGASUS Stable 1.0.1: opt-in one-shot Turkish speech command test.
Run from reconstructed Android project root after stable signing patch."""
from pathlib import Path

def replace(path, old, new, count=1):
    p = Path(path)
    body = p.read_text(encoding="utf-8")
    if body.count(old) != count:
        raise RuntimeError("Expected patch anchor count " + str(count) + " in " + str(p) + ": " + old[:100])
    p.write_text(body.replace(old, new, count), encoding="utf-8")

java = Path("app/src/main/java/ai/pegasus/nexus")
java.joinpath("PegasusVoice.kt").write_text(r'''package ai.pegasus.nexus

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/** One explicit press = one recognition session. No hotword or hidden recording. */
class PegasusVoice(
    private val context: Context,
    private val status: (String) -> Unit,
    private val command: (String) -> Unit
) {
    private var recognizer: SpeechRecognizer? = null
    private var listening = false

    fun start() {
        if (listening) {
            status("Zaten dinliyorum · bitir veya DUR'a bas")
            return
        }
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            status("Mikrofon izni gerekli · PEGASUS ana ekranından izin ver")
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            status("Cihazda konuşma tanıma hizmeti bulunamadı")
            return
        }
        try {
            val sr = SpeechRecognizer.createSpeechRecognizer(context)
            recognizer = sr
            listening = true
            sr.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = status("Dinliyorum · tek komut söyle")
                override fun onBeginningOfSpeech() = status("Ses algılandı")
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() = status("Komut çözümleniyor")
                override fun onError(error: Int) {
                    if (!listening) return
                    listening = false
                    release()
                    status("Ses tanınamadı (hata " + error + ") · tekrar Konuş'a bas")
                }
                override fun onResults(results: Bundle?) {
                    if (!listening) return
                    listening = false
                    release()
                    val heard = results?.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                    )?.firstOrNull().orEmpty().trim()
                    if (heard.isBlank()) status("Komut duyulmadı · tekrar dene")
                    else command(heard)
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            }
            sr.startListening(intent)
            status("Mikrofon açılıyor · konuşabilirsin")
        } catch (e: Exception) {
            listening = false
            release()
            status("Mikrofon başlatılamadı · hizmeti kontrol et")
        }
    }

    private fun release() {
        val current = recognizer
        recognizer = null
        try { current?.destroy() } catch (_: Exception) {}
    }

    fun stop() {
        listening = false
        val current = recognizer
        recognizer = null
        try { current?.cancel() } catch (_: Exception) {}
        try { current?.destroy() } catch (_: Exception) {}
    }
}
''', encoding="utf-8")

service = java / "PegasusAccessService.kt"
replace(service, "    private val main = Handler(Looper.getMainLooper())",
        "    private val main = Handler(Looper.getMainLooper())\n    private var voiceListener: PegasusVoice? = null")
replace(service, "    fun emergencyStop() {\n        stopped = true",
        "    fun emergencyStop() {\n        voiceListener?.stop()\n        stopped = true")
replace(service, '        actionRow("Kaydır" to { scrollDown() }, "DUR" to { emergencyStop() })',
        '        actionRow("Kaydır" to { scrollDown() }, "DUR" to { emergencyStop() })\n        actionRow("🎙 Konuş" to { startVoice() })')
replace(service, "    private fun updateText(s: String) {",
'''    private fun startVoice() {
        if (stopped) { updateText("Önce Başlat'a bas"); return }
        if (voiceListener == null) {
            voiceListener = PegasusVoice(this, { updateText(it) }, { applyVoiceCommand(it) })
        }
        voiceListener?.start()
    }

    private fun applyVoiceCommand(heard: String) {
        val cleaned = heard.lowercase(java.util.Locale("tr", "TR"))
            .replace(Regex("[.,!?]"), "").trim()
            .removePrefix("pegasus").trim()
        when (cleaned) {
            "dur", "durdur", "acil dur" -> emergencyStop()
            "tara", "ekranı tara", "ekran tara" -> scan()
            "sonraki", "ileri", "sonraki öğe", "sağa geç", "sağ" -> select(1)
            "önceki", "geri", "önceki öğe", "sola geç", "sol" -> select(-1)
            "dokun", "tıkla", "seçilene dokun" -> clickSelected()
            "kaydır", "aşağı kaydır", "aşağıya kaydır" -> scrollDown()
            else -> updateText("Anlaşılmayan komut · Tara / Sonraki / Dokun / Kaydır / Dur")
        }
    }

    private fun updateText(s: String) {''')

replace("app/src/main/AndroidManifest.xml", "    <application",
'''    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <queries>
        <intent><action android:name="android.speech.RecognitionService" /></intent>
    </queries>
    <application''')

activity = java / "MainActivity.kt"
replace(activity, "import android.os.Bundle",
    "import android.os.Bundle\nimport android.Manifest\nimport android.content.pm.PackageManager")
replace(activity, "    private inner class NativeBridge {",
'''    private inner class NativeBridge {
        @JavascriptInterface
        fun requestVoicePermission() {
            runOnUiThread {
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 6101)
                } else {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("PEGASUS mikrofonu hazır")
                        .setMessage("Diğer uygulamaya geçip P panelinde Başlat ve ardından Konuş'a bas. Sürekli dinleme etkin değildir.")
                        .setPositiveButton("Tamam", null)
                        .show()
                }
            }
        }
''')
html = Path("app/src/main/assets/nexus.html")
old = '<div class="action-row" style="margin-top:13px">'
new = '''<div class="action-row" style="margin-top:10px"><button class="btn ghost" style="width:100%" onclick="if(window.PegasusNative){window.PegasusNative.requestVoicePermission()}">🎙 Mikrofon izni · Sesli komut testi</button></div>
''' + old
replace(html, old, new)
replace("app/build.gradle.kts", "versionCode = 500", "versionCode = 501")
replace("app/build.gradle.kts", 'versionName = "1.0.0-stable"',
        'versionName = "1.0.1-voice-test"')

# Build gates: preserve exact package and signingConfig; new permissions do not remove old ones.
gradle = Path("app/build.gradle.kts").read_text()
manifest = Path("app/src/main/AndroidManifest.xml").read_text()
assert 'applicationId = "ai.pegasus.nexus.stable"' in gradle
assert "signingConfigs.getByName" in gradle
assert "android.permission.RECORD_AUDIO" in manifest
assert "PEGASUS" in service.read_text()
print("PEGASUS_STABLE_VOICE_PATCH=PASS")
