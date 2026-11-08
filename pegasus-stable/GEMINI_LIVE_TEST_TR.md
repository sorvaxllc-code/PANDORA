# PEGASUS Stable 1.0.2 — Gemini Live sesli oturum testi

## Ne tamamlandı?
- PEGASUS Stable applicationId: ai.pegasus.nexus.stable
- versionCode 502; v1.0.1=501 ve v1.0.0=500 üzerine imzalı güncelleme.
- NEXUS ekran tasarımı, P kontrol paneli, erişilebilirlik servisi ve önceki tek seferlik Türkçe ses komutları korunur.
- Ayarlar > Gemini Live deneme bağlantısı: kısa ömürlü auth_tokens/... oturum anahtarıyla WebSocket bağlantısı.
- Oturum kurulunca mikrofon PCM 16 kHz ile ses gönderir, 24 kHz PCM yanıtı çalar.
- Başka uygulamaya geçince veya DUR'a basınca Live ses oturumu kapanır; arka planda dinleme yok.
- Yapay zekâya ekran görüntüsü, rehber, mesaj geçmişi veya erişilebilirlik ağacı gönderilmez; Live kendi başına dokunma, kaydırma, arama, mesaj veya finans işlemi gerçekleştirmez.

## Nasıl test edilir?
1. Mevcut PEGASUS Stable APK'yı kaldırma; 1.0.2 APK dosyasına dokun ve **Güncelle**.
2. Mikrofon izni yalnızca ilk kez veya Android'in gerekli gördüğü durumlarda istenir.
3. Gemini Developer API erişimini kendi Google AI Studio hesabında yapılandır; kullanım maliyeti/limitleri hesabına göre değişebilir.
4. Android Termux üzerinde Python varsa token oluşturma aracıyla **yalnızca kısa ömürlü** token al. Kalıcı GEMINI_API_KEY'i APK'ya, sohbete, GitHub'a veya ekran görüntüsüne koyma.
5. PEGASUS > Ayarlar > Gemini Live > auth_tokens/... alanına geçici tokenı yazıp **Gemini'ye bağlan**.
6. Uygulama ön plandayken "Merhaba PEGASUS" diyerek dene; kulaklıktan yanıt geliyorsa işaretle.
7. **DUR · Kapat** tuşuna basıp mikrofon simgesinin kapandığını kontrol et.
8. Başka uygulamaya geçince sesli oturumun kapanması beklenir. P kontrol panelindeki bir önceki ses komutları ayrı çalışır.

## Token oluşturma
GitHub'daki pegasus-stable/gemini_token.py, Google Gemini API token uç noktasını kullanarak 60 saniye bağlantı başlatma ve 15 dakika oturum geçerliliği olan 1 kez kullanılabilir bir token üretir. Tokenı üretince gecikmeden yapıştır.
- Termux: python3 gemini_token.py (GEMINI_API_KEY ortam değişkeninde tanımlı olmalı).
- Termux komut geçmişine API anahtarını düz metin olarak yazma. Kendi özel ortamında güvenli şekilde tanımla.
- Token alımı veya WebSocket modeli reddedilirse hata ekranını paylaş ama anahtarları paylaşma.

## Bilinen sınırlar / canlıda doğrulama
- Bu test sürümünde token otomatik yenilenmez ve uygulama ön plandan çıkınca Live konuşma durur; mevcut önceki sesli ekran kontrol komutları değişmez.
- Sunucu tarafı kimlik doğrulaması, arka plan sesli konuşma, session resumption, tool calling, "Hey PEGASUS", hassas eylem onayı, ses gecikme ve bluetooth yönlendirme saha testleri henüz tamamlanmadı.
- Token geçerliliği, servis kullanılabilirliği ve Google hesabındaki Gemini Live model erişimi gerçek bağlantı ile doğrulanmalıdır. CI build başarı sonucu **telefon üzerinde Gemini konuşması çalışıyor** anlamına gelmez.
- Bu ilk sürümde model sabiti: gemini-3.8-live (Google'ın güncel Live dökümanlarından). Model erişimin yoksa oturum hata verebilir.
- Sürekli dinleme yoktur; mikrofon yalnızca açık Live oturumunda ve uygulama ön plandayken çalışır.
- Güvenli test için finansal işlemler, hesap şifreleri, kişiler ve özel mesajlar üzerinde otomasyon yapma.

Referans:
- https://ai.google.dev/gemini-api/docs/live-api/get-started-websocket
- https://ai.google.dev/gemini-api/docs/live-api/ephemeral-tokens
