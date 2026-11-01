# PANDORA 19 — Android + Hermes deneme sürümü

Bu, telefondaki Termux/Hermes sunucusuna bağlanan ayrı bir yerel Android uygulamasıdır. PANDORA 17 üzerine kurulmaz ve onu silmez.

## Test özellikleri
- Android'in kendi arayüzüyle siyah-beyaz sohbet
- Yerel Hermes: http://127.0.0.1:8642
- /v1/models bağlantı testi ve /v1/chat/completions sohbet
- Android Keystore ile şifreli API anahtarı
- Yeni oturum, Türkçe ses tanıma, isteğe bağlı seslendirme

## Android telefondan kurulum
1. Termux'ta Hermes Gateway açık olsun: hermes gateway
2. GitHub Actions üzerinden PANDORA 19 Hermes Android APK çalışmasının tamamlanmasını kontrol et.
3. Başarılı çalışmanın PANDORA-19-Hermes-APK adlı artifact ZIP dosyasını indir, içindeki app-debug.apk dosyasını aç.
4. Ayrı PANDORA 19 uygulamasını yükle.
5. AYAR ekranına Hermes API_SERVER_KEY değerini yapıştır. Bu anahtar Termux'un ~/.hermes/.env dosyasında. Anahtarı ChatGPT konuşmasına, GitHub'a veya ekran görüntüsüne gönderme.
6. BAĞLANTI TESTİ düğmesine bas. Başarılıysa kısa Türkçe bir mesaj gönder.

Termux'ta sadece kendi cihazının ekranında API anahtarını görüntülemek için:

    sed -n 's/^API_SERVER_KEY=//p' "$HOME/.hermes/.env" | tail -n 1

## Sınırlar ve güvenlik
- Bu henüz mevcut PANDORA 17'nin tüm özelliklerini içermeyen bir yerel Android denemesidir.
- WhatsApp, arama yönetimi, diğer uygulamaların kontrolü ve arka planda Hey Pandora henüz yok.
- Hermes Gateway Termux'ta çalışmadığında bağlantı kesilir.
- Sadece localhost'a HTTP izni verilir; uygulama internetten açık Hermes API sunucusuna bağlanmaz.
- API anahtarı hiçbir kaynak dosyaya gömülmez. Hermes güçlü terminal/dosya araçlarına erişebilir; riskli komutları onaysız çalıştırmayın.
- APK yalnızca GitHub Actions derlemesi başarılıysa hazır demektir; fiziksel telefon testi ayrıca gereklidir.

## Derleme
    gradle -p hermes-preview --no-daemon :app:assembleDebug
