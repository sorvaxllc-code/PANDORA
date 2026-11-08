# PEGASUS Stable 1.0.10 — Şeffaf 3B P hologramı

## İstek
Kullanıcının sağladığı NEMESIS-CodeOnTheGo-APK-Projesi.zip dosyasındaki yerel `overlay_orb.html` hologram çiziminden yararlanılarak, mevcut **PEGASUS NEXUS temasını değiştirmeden**, ortada N yerine **P** bulunan bir yapay zekâ çalışma göstergesi oluşturuldu. Uygulamada daha önce bulunan `pegasus_activity_orb.html` animasyonu korunur; 1.0.10 bu çizimi bağımsız ve sistem üstü Android penceresi olarak gösterir.

## Nasıl çalışır?
- Gemini Live oturumu kullanıcı tarafından görünür PEGASUS ekranından başlatılır.
- Uygulama arka plana geçince oturumu taşıyan **PegasusLiveService**, `SYSTEM_ALERT_WINDOW` izni varsa `TYPE_APPLICATION_OVERLAY` tipinde şeffaf 92dp'lik P göstergesi açar.
- Hologram yalnızca aktif ses oturumu sırasında görünür. Ses durduğunda, bildirimden DUR'a basıldığında veya servis kapandığında kaldırılır.
- P animasyonu tamamen yerel `file:///android_asset/pegasus_activity_orb.html` dosyasından yüklenir, içerik aktarımı yapmaz.
- Hologram dokunulamaz (`FLAG_NOT_TOUCHABLE`) ve odak alamaz (`FLAG_NOT_FOCUSABLE`), normal uygulama hareketlerini engellemez. Orijinal küçük P kontrol panelinden **bağımsızdır**.
- NEXUS içindeki mevcut ayarlar yalnızca iki düğme adında güncellenir: **3B P hologramını aç / 3B P hologramını gizle**. Ana sayfa yerleşimi, teması, Gemini Live bağlantısı ve ekran araçları değiştirilmedi.
- Ekran taramasına dair 1.0.8/1.0.9 izin / tanılama iyileştirmeleri aynı pakette kalır.

## Android izin kurulumu
1. Önceki PEGASUS'u silmeden 1.0.10 APK dosyasını **Güncelle** olarak kur.
2. PEGASUS'u aç ve **Ayarlar → 3B P hologramını aç** düğmesine bas.
3. Açılan izin açıklamasındaki **İzin ayarlarına git** düğmesine bas.
4. Android'de **Diğer uygulamaların üzerinde göster** (veya Xiaomi/HyperOS'ta benzer isimli özel erişim) izin sayfasından **PEGASUS** için izni elle etkinleştir.
5. PEGASUS'a geri dön. **Sürekli ses oturumunu başlat**.
6. Hesap makinesine veya ana ekrana geç. Hareketli P hologramı diğer uygulamanın üzerinde görünmeli.
7. Bildirimden **DUR · Mikrofonu kapat** seçeneğini kullan. Hologram kaybolmalı.
8. NEXUS içindeki **3B P hologramını gizle** düğmesi çalışırken simgeyi kapatmalı, mikrofon oturumunu durdurmamalı.

## Sınırlar
- Android bazı hassas veya sistem ekranlarında, izin verilmiş olsa da üçüncü taraf overlay'leri engelleyebilir; bu bilerek korunur. Hiçbir güvenlik kısıtlaması aşılmaz.
- Android uygulaması özel `SYSTEM_ALERT_WINDOW` iznini kendi kendine veremez. Kullanıcı elle açmalı.
- Hologram 3B görünüm veren bir **2B canvas animasyonudur**; gerçek uzamsal hologram veya kamera üstü AR değildir.
- Başka uygulamalar üzerinde görünebilmesi, tüm uygulamalarda içerik tarama veya kontrol izni verildiği anlamına gelmez. Erişilebilirlik / gizlilik izinleri ayrıca geçerlidir.
- Sürekli ses oturumu pil, veri kullanımı ve Gemini API kotası etkilerine sahiptir; kullanıcı istediği an DUR ile kesebilir.
- CI başarılı olsa bile gerçek telefon gösterimi ayrıca kullanıcı tarafından test edilmelidir.

## Dağıtım ve güncelleme kimliği
- Android paket kimliği: `ai.pegasus.nexus.stable`
- versionCode: `510` / Version: `1.0.10-system-alert-hologram`
- Aynı kalıcı imza sertifikası SHA-256: `3e4fc38052b9dbd7bb9324158303817d4b2015315737e2983af49458a2d73afc`
- 1.0.11'e imza / güncelleme uyumluluğu CI'da ayrıca test edilir.

Resmî Android referansları:
- https://developer.android.com/reference/android/Manifest.permission#SYSTEM_ALERT_WINDOW
- https://developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_APPLICATION_OVERLAY
