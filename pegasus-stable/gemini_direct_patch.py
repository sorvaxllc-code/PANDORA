#!/usr/bin/env python3
"""After PEGASUS 1.0.2 scripts, add direct Gemini Live (1.0.3).
The user enters their own Google AI Studio API key once. Store it encrypted
using Android Keystore (AES-GCM), never commit or log it, and only connect
on explicit user request. No backend required for this PERSONAL TEST mode.
"""
from pathlib import Path

def repl(p, old, new, n=1):
    file = Path(p)
    s = file.read_text(encoding="utf-8")
    count = s.count(old)
    if count != n:
        raise RuntimeError(f"{p}: patch anchor count {count} != {n}: {old[:140]}")
    file.write_text(s.replace(old, new, n), encoding="utf-8")

base=Path("app/src/main/java/ai/pegasus/nexus")
base.joinpath("PegasusApiKeyVault.kt").write_text(r'''package ai.pegasus.nexus

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** On-device, encrypted-only storage for a user-owned Google AI Studio API key.
 * Android Keystore non-exportable AES key; no backup, logs, or JS getter.
 * Personal test only: backend-generated ephemeral tokens are preferred for production.
 */
internal object PegasusApiKeyVault {
    private const val ALIAS = "PEGASUS_GEMINI_DIRECT_V1"
    private const val PREFS = "pegasus_private_live_v1"
    private const val IV = "iv"
    private const val DATA = "value"

    private fun key(): SecretKey {
        val store=KeyStore.getInstance("AndroidKeyStore")
        store.load(null)
        val old=store.getKey(ALIAS, null) as? SecretKey
        if (old != null) return old
        val generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore")
        val spec=KeyGenParameterSpec.Builder(
            ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setKeySize(256)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    fun save(context: Context, raw: String): Boolean {
        val v=raw.trim()
        if (v.length !in 30..160 || !v.startsWith("AIza") ||
            v.any { !(it.isLetterOrDigit() || it == '_' || it == '-') }) return false
        return try {
            val cipher=Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key())
            val encrypted=cipher.doFinal(v.toByteArray(Charsets.UTF_8))
            context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit()
                .putString(IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                .putString(DATA, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .apply()
            true
        } catch (_: Exception) { false }
    }

    fun load(context: Context): String? {
        return try {
            val pref=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
            val iv=pref.getString(IV,null) ?: return null
            val data=pref.getString(DATA,null) ?: return null
            val store=KeyStore.getInstance("AndroidKeyStore")
            store.load(null)
            val secret=store.getKey(ALIAS,null) as? SecretKey ?: return null
            val cipher=Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secret,
                GCMParameterSpec(128, Base64.decode(iv,Base64.DEFAULT)))
            String(cipher.doFinal(Base64.decode(data,Base64.DEFAULT)), Charsets.UTF_8)
        } catch (_: Exception) { null }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().clear().apply()
        try {
            val store=KeyStore.getInstance("AndroidKeyStore")
            store.load(null)
            if (store.containsAlias(ALIAS)) store.deleteEntry(ALIAS)
        } catch (_: Exception) {}
    }
}
''',encoding="utf-8")

live=base/"PegasusGeminiLive.kt"
repl(live, "fun start(ephemeralToken: String) {", "fun start(apiKey: String) {")
repl(live, '''if (ephemeralToken.length !in 20..1000 || !ephemeralToken.startsWith("auth_tokens/")) {
            status("Geçerli kısa ömürlü Gemini oturum anahtarı gerekli"); return
        }''',
'''if (apiKey.length !in 30..160 || !apiKey.startsWith("AIza")) {
            status("Google AI Studio API anahtarı gerekli · Ayarlar'dan kaydet"); return
        }''')
repl(live, '''"google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContentConstrained" +
                "?access_token=" + URLEncoder.encode(ephemeralToken, "UTF-8")''',
''' "google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent" +
                "?key=" + URLEncoder.encode(apiKey, "UTF-8")''')
repl(live, '.put("responseModalities", JSONArray().put("AUDIO"))',
    '.put("generationConfig", JSONObject().put("responseModalities", JSONArray().put("AUDIO")))')
repl(live, 'if (data.isNotEmpty() && mime.startsWith("audio/pcm")) {',
'''if (data.isNotEmpty() && (mime.startsWith("audio/pcm", true) ||
                                    mime.startsWith("audio/L16", true))) {''')
repl(live, '''                                playbackQueue.offer(bytes)
''', '''                                playbackQueue.offer(bytes)
                                receivedAudioChunks++
                                if (receivedAudioChunks == 1) {
                                    status("Gemini ses yanıtı gönderdi · hoparlöre aktarılıyor")
                                }
''')
repl(live,'    private var generation = 0', '''    private var generation = 0
    private var receivedAudioChunks = 0''')
repl(live,'        active = true\n        ready = false',
    '        active = true\n        ready = false\n        receivedAudioChunks = 0')
repl(live,'    fun connected(): Boolean = ready && active',
'''    fun connected(): Boolean = ready && active

    /** Tests the model's audio output without relying on microphone VAD. */
    fun testSpeechOutput() {
        if (!connected()) { status("Test sesi için önce Gemini'ye bağlan"); return }
        val turn = JSONObject().put("role", "user")
            .put("parts", JSONArray().put(JSONObject().put("text",
                "Ses çıkışı kontrolü: Lütfen Türkçe tek bir cümle sesli söyle: Merhaba, ben PEGASUS.")))
        val payload = JSONObject().put("clientContent", JSONObject()
            .put("turns", JSONArray().put(turn))
            .put("turnComplete", true))
        if (ws?.send(payload.toString()) == true)
            status("Gemini'ye test isteği gönderildi · sesli yanıt bekleniyor")
        else status("Test isteği gönderilemedi")
    }''')
repl(live, '''                        main.post { stop() }
                    }
                }
                override fun onClosed''',
'''                        main.post {
                            stop()
                            status("Gemini bağlantı hatası" +
                                (response?.code?.let { " · HTTP $it" } ?: "") +
                                " · internet veya API erişimini kontrol et")
                        }
                    }
                }
                override fun onClosed''')
repl(live, '''                        main.post { stop() }
                    }
                }
            })''',
'''                        main.post {
                            stop()
                            status("Gemini bağlantısı kapandı ($code) · yeniden bağlan")
                        }
                    }
                }
            })''')

activity=base/"MainActivity.kt"
repl(activity,'''        fun startGemini(token: String) {
            runOnUiThread { live.start(token.trim()) }
        }''',
'''        fun saveGeminiApiKey(apiKey: String) {
            runOnUiThread {
                val ok = PegasusApiKeyVault.save(this@MainActivity, apiKey)
                liveStatus(if (ok) "API anahtarı cihazda şifrelendi · şimdi Bağlan'a bas"
                           else "Anahtar kaydedilemedi · Google AI Studio anahtarını kontrol et")
            }
        }

        @JavascriptInterface
        fun startGeminiDirect() {
            runOnUiThread {
                val saved = PegasusApiKeyVault.load(this@MainActivity)
                if (saved.isNullOrEmpty()) liveStatus("Önce API anahtarını bir kez kaydet")
                else live.start(saved)
            }
        }

        @JavascriptInterface
        fun hasGeminiKey(): Boolean =
            PegasusApiKeyVault.load(this@MainActivity) != null

        @JavascriptInterface
        fun forgetGeminiKey() {
            runOnUiThread {
                live.stop()
                PegasusApiKeyVault.clear(this@MainActivity)
                liveStatus("API anahtarı cihazdan silindi · bağlantı kapalı")
            }
        }

        @JavascriptInterface
        fun testGeminiSpeech() {
            runOnUiThread { live.testSpeechOutput() }
        }''')
repl(activity,
'runOnUiThread { PegasusAccessService.active?.emergencyStop() }',
'runOnUiThread { PegasusAccessService.active?.emergencyStop(); if (::live.isInitialized) live.stop() }')

html=Path("app/src/main/assets/nexus.html")
repl(html,'''Güvenlik için yalnızca kısa ömürlü bir oturum anahtarı kabul edilir. Google API anahtarını buraya yazma. Oturum sadece PEGASUS ekrandayken açıktır; başka uygulamaya geçtiğinde mikrofon kapanır.''',
'''Kişisel test: Google AI Studio API anahtarını yalnızca bir kez kaydet. Anahtar Android Keystore ile şifrelenir, GitHub'a gönderilmez. Kalıcı API anahtarıyla doğrudan bağlantı, sunucudan geçici anahtar almaktan daha risklidir. Ses yalnızca PEGASUS açıkken gönderilir.''')
repl(html,'''<input class="input" type="password" id="gemini-ephemeral" autocomplete="off" placeholder="auth_tokens/... geçici anahtar">''',
'''<input class="input" type="password" id="gemini-api-key" autocomplete="off" autocapitalize="off" spellcheck="false" placeholder="Google AI Studio API anahtarın (AIza...)">
  <div class="action-row" style="margin-top:9px">
    <button class="btn ghost" id="gemini-save-key" style="flex:1">Anahtarı kaydet</button>
    <button class="btn ghost" id="gemini-forget-key" style="flex:1">Anahtarı sil</button>
  </div>''')
repl(html,'''<button class="btn primary" id="gemini-connect" style="flex:1">Gemini'ye bağlan</button>''',
    '''<button class="btn primary" id="gemini-connect" style="flex:1">Gemini Live'a bağlan</button>''')
repl(html,'''<p id="gemini-session-status" class="helper" style="margin-top:8px">Gemini Live bağlı değil · geçici anahtar gerekli</p>''',
'''<button class="btn ghost block" id="gemini-test-speech" style="margin-top:8px;width:100%">Ses çıkışını dene · PEGASUS konuşsun</button>
  <p id="gemini-session-status" class="helper" style="margin-top:8px">Gemini bağlı değil · önce anahtarı kaydet</p>''')
repl(html,'''document.querySelector('#gemini-connect').addEventListener('click',()=>{
  const input=document.querySelector('#gemini-ephemeral');
  const key=input.value.trim(); input.value='';
  if(window.PegasusNative)window.PegasusNative.startGemini(key);
  else window.pegasusGeminiStatus('Bu işlev yalnızca Android uygulamasında çalışır');
});''',
'''document.querySelector('#gemini-save-key').addEventListener('click',()=>{
  const input=document.querySelector('#gemini-api-key');
  const key=input.value.trim(); input.value='';
  if(window.PegasusNative)window.PegasusNative.saveGeminiApiKey(key);
  else window.pegasusGeminiStatus('Yalnızca Android uygulamasında kullanılabilir');
});
document.querySelector('#gemini-forget-key').addEventListener('click',()=>{
  document.querySelector('#gemini-api-key').value='';
  if(window.PegasusNative)window.PegasusNative.forgetGeminiKey();
});
document.querySelector('#gemini-connect').addEventListener('click',()=>{
  if(window.PegasusNative)window.PegasusNative.startGeminiDirect();
  else window.pegasusGeminiStatus('Bu işlev yalnızca Android uygulamasında çalışır');
});
document.querySelector('#gemini-test-speech').addEventListener('click',()=>{
  if(window.PegasusNative)window.PegasusNative.testGeminiSpeech();
});''')
repl(html,'''Gemini Live isteğe bağlı sesli oturum: Ayarlar bölümünden geçici anahtarla bağlan.''',
    '''Gemini Live isteğe bağlı sesli oturum: Ayarlar bölümünde anahtarını bir kez kaydet, sonra tek dokunuşla bağlan.''')
repl("app/build.gradle.kts", "versionCode = 502", "versionCode = 503")
repl("app/build.gradle.kts", 'versionName = "1.0.2-live-session-test"',
    'versionName = "1.0.3-direct-live-test"')

gradle=Path("app/build.gradle.kts").read_text()
manifest=Path("app/src/main/AndroidManifest.xml").read_text()
kotlin=live.read_text()
assert 'applicationId = "ai.pegasus.nexus.stable"' in gradle
assert 'signingConfigs.getByName("pegasusStable")' in gradle
assert 'versionCode = 503' in gradle
assert 'android.permission.RECORD_AUDIO' in manifest and 'android.permission.INTERNET' in manifest
assert 'BidiGenerateContentConstrained' not in kotlin
assert 'BidiGenerateContent"' in kotlin
assert 'generationConfig' in kotlin
assert 'fun testSpeechOutput()' in kotlin
assert 'fun startGeminiDirect()' in activity.read_text()
assert 'gemini-ephemeral' not in html.read_text()
print("PEGASUS_DIRECT_LIVE_503_PATCH=PASS")
