# Kalici PANDORA imzalama kurulumu

Android, uygulama guncellemelerinde ayni paket kimligi ve ayni imza ister.
GitHub Actions gecici debug anahtari her seferinde degisebilir. Bu nedenle
mevcut PANDORA veya PANDORA 11 uygulamasini silme.

## GitHub otomatik derleme
Su an tum push'larda debug APK derlenir. Imzali stable APK derlemesi icin
iki sifreli GitHub Actions repository secret'i gerekir:

- PANDORA_KEYSTORE_BASE64: yerel olarak olusturulmus PKCS12 (.p12) imza dosyasinin base64 icerigi.
- PANDORA_KEYSTORE_PASSWORD: bu keystore'un parolasi.

Keystore icinde alias 'pandora' olmali, anahtar ve store parolasi ayni olmali.
Bu sirlar GitHub Settings > Secrets and variables > Actions uzerinden
ayarlanir. Gizli anahtari asla kaynak depoya, sohbete veya bir commit'e koyma.

Ilk stable APK kimligi com.jarvis.kisisel.stable'dir. Eski PANDORA'ya
guncelleme olarak kurulmaz; sadece ilk defa AYRI uygulama olarak yuklenir.
Bundan sonraki stable APK'lar bu kimligi, imzalama anahtarini korumali ve
her yayin versionCode degerini artirmalidir.

Yeni bir keystore olusturmak yerine var olan stable imza dosyasini her
yeni derlemede kullanmak esastir. Anahtar kaybolursa ilerideki guncellemeler
mevcut stabil uygulamanin ustune kurulamayabilir. Guvenli yedek al.

Notlar ve API ayarlari paketler arasinda otomatik tasinmaz. Kullanici
onayiyla yedekleme / icerik aktarimi yapilmalidir.

GitHub CI simdilik iki secret saglanmazsa yalnizca debug APK uretir.
Bu, signing isleminin henuz tamamlandigi anlamina gelmez.