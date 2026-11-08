# PEGASUS Stable 1.0.4 — Gemini Auth Key desteği

**Sorun:** Google AI Studio 28 Mayıs 2026'dan itibaren yeni anahtarları auth key biçiminde üretmektedir. Bu anahtarların başlangıcı sıklıkla `AQ.` olur. PEGASUS Stable 1.0.3, hem anahtarı kaydederken hem bağlantıyı başlatırken yalnızca `AIza` önekini kabul ettiğinden geçerli AQ. anahtarları yerel olarak reddediyordu.

**Düzeltme:** 1.0.4 cihazda şifrelenerek saklanan anahtar için hem `AQ.` hem `AIza` biçimini kabul eder. Bu yalnızca yerel biçim kontrolüdür; Google sunucusu ilgili anahtarın kullanım yetkisini ayrıca doğrular. Anahtar APK içinde gömülü değildir.

**Kullanım:** Önceki Stable uygulamasını silme. 1.0.4 APK'yı mevcut uygulamaya **Güncelle** ile kur. Ardından PEGASUS > Ayarlar > Gemini Live'daki eski/boş anahtar girişini `AQ.` veya `AIza` ile başlayan **kendi Google AI Studio API anahtarınla** doldur ve **Anahtarı kaydet** düğmesine dokun. Sonra **Gemini Live'a bağlan** → **Ses çıkışını dene**.

Yeni anahtar gerekiyorsa https://aistudio.google.com/api-keys sayfasından **Create API Key** seçeneğini kullan. Zaten geçerli `AQ.` anahtarın varsa tekrar oluşturman gerekmez.

**Güvenlik:** Anahtarını sohbette, ekran görüntüsünde veya GitHub'a gönderme. Uygulama Android Keystore AES-GCM kullanır; ancak kalıcı anahtarın doğrudan bir Android istemcisinde tutulması üretim sistemleri için önerilmez. Üretimde kimlik doğrulamalı backend ile kısa ömürlü token kullanılmalıdır.

**Kontrol:** Gemini bağlantısı başarılı görünse bile mikrofon kaydı ve hoparlörden gerçek ses çıkışı ayrı ayrı gerçek telefonda doğrulanmalıdır.

Resmî belgeler:
- https://ai.google.dev/gemini-api/docs/api-key
- https://ai.google.dev/gemini-api/docs/live-api/get-started-websocket
