# PEGASUS Stable 1.0.3 — Doğrudan Gemini Live (kişisel test)

## Amaç
Google AI Studio'dan alınan kendi API anahtarını bir kez uygulamada **Ayarlar → Gemini Live** ekranına kaydet. Bundan sonraki oturumlarda geçici token kopyalama veya Termux'a geçme ihtiyacı olmasın. Kullanıcı, **Gemini Live'a bağlan** ve **DUR** ile oturumu kontrol eder.

Bu entegrasyon Google'ın **Gemini Live Developer API** hizmetini kullanır; ayrı Gemini mobil uygulamasının kapalı kaynak Live ekranına bağlanmaz.

## Kurulum ve test
1. Mevcut PEGASUS Stable'ı kaldırma. Yeni `PEGASUS-STABLE.apk` dosyasında **Güncelle** seç.
2. Ayarlar bölümündeki **Gemini Live** kartını aç.
3. **Google AI Studio API anahtarını** bir kez yaz ve **Anahtarı kaydet** seç. Anahtarını bu sohbete, GitHub'a veya ekran görüntüsüne gönderme.
4. **Gemini Live'a bağlan** düğmesine bas. Beklenen durum: `Gemini Live bağlandı` sonra `Gemini Live · mikrofon aktif`.
5. **Ses çıkışını dene** düğmesine bas. Bu komut mikrofon gerektirmeden Gemini'den kısa Türkçe sesli yanıt ister. Böylece hoparlör/PCM yolu test edilir.
6. Sesli yanıt gelirse **Merhaba PEGASUS, beni duyuyor musun?** de ve normal mikrofon konuşmasını test et.
7. **DUR · Kapat** ile oturumu kapat. Ardından tekrar **Gemini Live'a bağlan** komutuyla, Termux'a dönmeden tekrar bağlanmayı dene.
8. Başka uygulamaya geçince açık oturumun **durması** beklenir. Android bildirim/arka plan ses hizmeti ileride ayrıca ele alınır.

## Sorun olursa (sırayla)
- `Mikrofon aktif` yoksa mikrofon Android iznini ve AudioRecord erişimini kontrol et.
- `Gemini ses yanıtı gönderdi` yazıp ses duyulmuyorsa medya ses düzeyi, Bluetooth/ahize yönlendirmesi ve AudioTrack çıkışını kontrol et.
- `Test isteği gönderildi` yazıyor ama yanıt paketi gelmiyorsa API kotası, model erişimi, ses yapılandırması veya bağlantı olaylarını kontrol et.
- HTTPS/WebSocket bağlantı hatasında hata kodunun ekran görüntüsünü paylaşabilirsin, **anahtarını karala**.
- `Bağlandı` yazması yalnızca ağ ve kurulum el sıkışmasının başarılı olduğunu gösterir; **gerçek ses gönderimi ve oynatımı ayrıca test edilmelidir.**

## Güvenlik ve sınırlar
- Anahtar APK içine gömülü değil. Kullanıcının girdiği anahtar Android Keystore AES-GCM ile cihaz üzerinde şifrelenir; erişim uygulama kapsamındadır.
- Bu yöntem kişisel testleri kolaylaştırır fakat Google'a doğrudan bağlanırken kalıcı anahtar cihaz belleğinde bulunur ve erişimi kötüye kullanım riskine açıktır. Üretim için kimlik doğrulamalı backend + otomatik kısa ömürlü token tavsiye edilir.
- API ücretleri/kotaları Google projesine bağlıdır; kayıtlı anahtarı Google AI Studio üzerinden sınırlandırıp düzenli izle.
- Gemini Live yalnızca **PEGASUS ekrandayken**, açıkça başlatılmış oturumda konuşur; otomatik "Hey PEGASUS" ya da arka plan dinlemesi yoktur.
- Gemini bu aşamada cihazda kendiliğinden dokunma, arama, mesaj veya finans işlemi yapamaz. Ekran kontrolü önceki PEGASUS güvenli komut panelinden ayrı çalışır.
- Android kullanıcı verileri ve izinleri, aynı `ai.pegasus.nexus.stable` kimliği ve imza ile üzerine kurulumda normalde korunur.
- Bu sürüm için build / imza testleri gerçek Android telefon testinin yerine geçmez.

Resmî protokol:
https://ai.google.dev/gemini-api/docs/live-api/get-started-websocket
https://ai.google.dev/gemini-api/docs/live-api/ephemeral-tokens
