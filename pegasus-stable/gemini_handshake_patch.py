#!/usr/bin/env python3
"""PEGASUS 1.0.5: Live WebSocket handshake diagnostics / binary message support.

Applied after 1.0.4 auth-key patch. Preserve stable appId/signing and
foreground-only mic. Never expose credentials, request URLs or user audio logs.
"""
from pathlib import Path

def rep(path, old, new):
    f=Path(path)
    s=f.read_text(encoding="utf-8")
    n=s.count(old)
    if n != 1:
        raise RuntimeError(f"Unexpected patch anchor ({n}) in {path}: {old[:80]}")
    f.write_text(s.replace(old,new),encoding="utf-8")

src=Path("app/src/main/java/ai/pegasus/nexus/PegasusGeminiLive.kt")
rep(src,"import okhttp3.WebSocketListener","import okhttp3.WebSocketListener\nimport okio.ByteString")
rep(src,'''                    webSocket.send(setup.toString())
                    status("Bağlantı açıldı · oturum hazırlanıyor")''',
'''                    if (!webSocket.send(setup.toString())) {
                        status("Gemini kurulum isteği gönderilemedi")
                        main.post { stop(); status("Gemini kurulum isteği gönderilemedi") }
                        return
                    }
                    status("Gemini kurulum isteği gönderildi · onay bekleniyor")
                    // WebSocket open does not mean the server has accepted the model.
                    // Make a failed setup visible instead of leaving the UI stuck forever.
                    main.postDelayed({
                        if (active && !ready && generation == currentGeneration) {
                            stop()
                            status("Gemini oturum onayı gelmedi (15 saniye) · model veya bağlantıyı kontrol et")
                        }
                    }, 15000L)''')
rep(src,'''                override fun onMessage(webSocket: WebSocket, text: String) {
                    if (!active || generation != currentGeneration) return''',
'''                // Some compatible transport paths deliver JSON as binary WebSocket frames.
                // Without this overload the UI may remain at "oturum hazırlanıyor".
                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    if (bytes.size > 4 * 1024 * 1024) {
                        status("Gemini yanıtı çok büyük · oturumu kapat")
                        main.post { stop() }
                        return
                    }
                    onMessage(webSocket, bytes.utf8())
                }
                override fun onMessage(webSocket: WebSocket, text: String) {
                    if (!active || generation != currentGeneration) return''')
rep(src,'''                        val m = JSONObject(text)
                        if (m.has("setupComplete") && !ready) {''',
'''                        val m = JSONObject(text)
                        if (m.has("error")) {
                            val e = m.optJSONObject("error")
                            val code = e?.optInt("code", -1) ?: -1
                            main.post {
                                stop()
                                status("Gemini sunucu hatası" + (if (code > 0) " · kod $code" else "") +
                                    " · API anahtarı, model erişimi ve kota kontrol edilmeli")
                            }
                            return
                        }
                        if (m.has("setupComplete") && !ready) {''')
# Exception path must not overwrite meaningful connection issue with generic no-state message.
rep(src, '''                    } catch (_: Exception) {
                        status("Ses yanıtı işlenemedi")
                    }
                }
                override fun onFailure''',
'''                    } catch (_: Exception) {
                        if (!ready) status("Gemini kurulum yanıtı çözümlenemedi · protokol uyumsuzluğu")
                        else status("Gemini ses yanıtı çözümlenemedi")
                    }
                }
                override fun onFailure''')
# Existing onFailure posts status, then stop overwrites status. Ensure the reason remains visible.
rep(src,'''                    if (active && generation == currentGeneration) {
                        status("Gemini bağlantı hatası" + (response?.code?.let { " (HTTP $it)" } ?: "") +
                            " · anahtar, bağlantı veya model erişimini kontrol et")
                        main.post {
                            stop()
                            status("Gemini bağlantı hatası" +
                                (response?.code?.let { " · HTTP $it" } ?: "") +
                                " · internet veya API erişimini kontrol et")
                        }
                    }''',
'''                    if (active && generation == currentGeneration) {
                        val responseCode = response?.code
                        main.post {
                            stop()
                            status("Gemini bağlantı hatası" +
                                (responseCode?.let { " · HTTP $it" } ?: " · ağ bağlantısı") +
                                " · API anahtarı, model erişimi veya interneti kontrol et")
                        }
                    }''')
rep(src,'''                    if (active && generation == currentGeneration) {
                        status("Gemini Live bağlantısı kapandı ($code)")
                        main.post {
                            stop()
                            status("Gemini bağlantısı kapandı ($code) · yeniden bağlan")
                        }
                    }''',
'''                    if (active && generation == currentGeneration) {
                        main.post {
                            stop()
                            status("Gemini oturumu sunucu tarafından kapatıldı · kod $code" +
                                if (!ready) " · oturum onayı alınamadı" else " · yeniden bağlan")
                        }
                    }''')
p=Path("app/build.gradle.kts")
rep(p,"versionCode = 504","versionCode = 505")
rep(p,'versionName = "1.0.4-authkey-compat"','versionName = "1.0.5-live-diagnostics"')
gradle=p.read_text()
code=src.read_text()
assert 'applicationId = "ai.pegasus.nexus.stable"' in gradle
assert 'signingConfigs.getByName("pegasusStable")' in gradle
assert 'versionCode = 505' in gradle
assert 'generationConfig' in code and 'onMessage(webSocket: WebSocket, bytes: ByteString)' in code
assert 'main.postDelayed' in code and 'responseCode?.let' in code
print("PEGASUS_LIVE_HANDSHAKE_DIAGNOSTICS_PATCH=PASS")
