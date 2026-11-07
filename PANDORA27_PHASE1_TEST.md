# PANDORA 27 — AŞAMA 1 GERÇEK CİHAZ TESTİ

Bu test listesi **özellikle Android 13 ve kulak içi Bluetooth kulaklık** için hazırlanmıştır. Donanım testleri henüz otomatik CI'da yapılmış değildir. APK'yı mutlaka `PANDORA27-STABLE-APK` artifact'ından kur. Debug `.trial27` mevcut stable sürümün üstüne kurulmaz.

1. **Güncelleme ve veri:** APK kurulurken 'Güncelle' seçeneği çıkmalı. Eski notların, Groq/Hermes anahtarların ve sohbetlerin kalmalı. Android 'Uygulama yüklenemedi' derse **silme**; tam hata ekranını kaydet.
2. **Uyandırma:** Kulaklığı tak ve PANDORA'nın ses sistemini başlat. 'Pandora, saat kaç' ve 'Hey Pandora, saat kaç' de. Ses algılama ve Türkçe yanıtı kontrol et.
3. **Kulaklık kilidi:** Kulaklığı çıkar. Mikrofon 'Kulaklık bekleniyor' / kapalı durumuna geçmeli; telefon hoparlöründen PANDORA konuşmamalı. Yeniden bağla ve normale dönüşü dene.
4. **Sesli acil dur:** Bir AI yanıtı veya müdür görevi sürerken kulaklıktan yalnızca 'dur' de. Ses/görev kesilmesini kontrol et. Tekrar 'Hey Pandora' deyince kendiliğinden başlamamalı; uygulamadan dinlemeyi yeniden açman gerekir.
5. **Bildirimden acil dur:** Dinleme açıkken PANDORA kalıcı bildirimini genişlet ve **ACİL DUR** seçeneğine dokun. İşlem ve mikrofon kapanmalı.
6. **Onay güvenliği:** 'Pandora, [güvenli test kişisi] ara' de; 'aramayı iptal et' ile iptal edilebildiğini dene. Gerçek arama yapmadan önce 'aramayı onayla' diyerek çağrı açılmasını test etmek istersen **numarayı kontrol et** ve isteyerek onayla.
7. **Ekran okuma ve karo:** İzinli, şifresiz bir sayfada Hızlı Ayarlar → PANDORA · Ekrana Bak'ı dene. Erişilebilirlik izni açıkken mevcut ekranın metni özetlenebilmeli.
8. **BootReceiver:** İleri tarihli bir hatırlatıcı kaydet. Telefon yeniden başladığında hatırlatıcının kaybolmadığını kontrol et. Mikrofona otomatik başlama garantisi bekleme.
9. **Android Gelişmiş Koruma:** PANDORA → Sistem durumu / Kurulum Merkezi → Gelişmiş Koruma. Android 13'te '**tespit edilemiyor**' görmen doğrudur; **kapalı** dememeli. Android 16+ cihazında açıksa ayrıca uyarı beklenir.
10. **Geri dönüş:** Takılma/çökme olursa PANDORA 26'nın aynı sertifikayla imzalanmış stable APK'sına normal downgrade Android tarafından engellenebilir; yedeklemeden uygulamayı silme. Ekran görüntüsü, Android sürümü ve adımı paylaş.

## Sabit kalacaklar
- `OfflineVoice` Vosk TR, mevcut `WakeMonitorService`, `HeadsetAudioRoute`
- `PandoraAccessibilityService`, `PandoraQuickTileService`, `PandoraQuickActionActivity`
- `BootReceiver` ve `MemoryDb`
- `com.jarvis.kisisel.stable` paket kimliği; kalıcı `pandora` signing key alias, mevcut sertifika SHA-256 karşılaştırma

## Android Gelişmiş Koruma sınırı
`AdvancedProtectionManager.isAdvancedProtectionEnabled()` Android 16/API 36'dadır. Android 13/14/15 üzerinden cihaz koruma modunun durumunu güvenilir biçimde öğrenemeyiz. Android 16+ cihazda mod açıksa PANDORA uyarır, korumayı devre dışı bırakmaya çalışmaz.

## Herhangi bir aşamada başarısızlık
`https://github.com/sorvaxllc-code/PANDORA/actions` → **PANDORA 27** → **PANDORA27-RAW-LOGS** dosyası *ham test, APK derleme ve imza* çıktılarının tamamını içerir. Bütün dosyalar kaynak ZIP'inde ayrıca yer alır.
