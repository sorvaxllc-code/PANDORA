# PANDORA 10 — otomatik APK derleme altyapisi

Bu proje **kurulabilir APK degil, kaynak kod paketidir.** GitHub baglantisi ve ilk depo olusturma adimlari tamamlanmadan otomatik derleme baslamaz.

## Mevcut isleyis
- Android'de PANDORA uygulamasi siyah-beyaz; notlar, komutlar, OpenRouter ucretsiz/yerel Qwen baglantilari korunur.
- OpenRouter chat yaniti null/eksik geldigi zaman ekranda artik harfiyen `null` yazmaz. Anlasilir hata doner; bulut bir metin gercekten uretmediyse kaynak etiketi basarili cevap sayilmaz.
- Notlar bolumunun yaninda **Not yedegi** secenegi eklendi. Uygulama degistirme oncesi istek uzerine paylasarak sakla.

## Otomatik derleme — tek seferlik GitHub ayari
1. GitHub hesabini bagla; yalnizca senin kontrolundeki bir repoda PANDORA kaynak kodu tutulur. Ilk etapta **ozel (private)** repo tercih et.
2. Bu ZIP'in icindeki dosyalari (ustte app, .github, build.gradle, settings.gradle) o repoya yukle. `.github/workflows/apk.yml` gizli dosya/klasor yolunu da koru.
3. `main` dalina her guncellemede GitHub Actions otomatik **debug APK** derler ve Actions > Artifacts altinda saklar. Debug APK her calistirmada degisebilen imza nedeniyle kalici guncelleme kanali degildir.
4. Gercek telefon guncellemeleri icin bir defalik **kalici imzalama anahtari** gerekir. Bu anahtar cihazdan/IDE'den/CI'den elde edilirse guvenli yedegini al. GitHub repo ayarlarinda Actions secrets olarak su dort ad gerekir: `PANDORA_KEYSTORE_BASE64`, `PANDORA_KEYSTORE_PASSWORD`, `PANDORA_KEY_ALIAS`, `PANDORA_KEY_PASSWORD`. **Anahtari ve parolalari sohbete gonderme, repoya koyma.**
5. Gerekli secrets kurulduktan sonra `v1.0.0` gibi bir Git tag'i release derlemesini ve GitHub Releases'a imzali APK yuklemesini baslatir. Surum numaralari (versionCode) her sonraki guncellemede artirilmalidir.

**KRITIK:** Code on the Go ile kurulu eski PANDORA uygulamasi muhtemelen farkli bir debug anahtariyla imzalandi. Yeni release imzasi farkliysa Android eski uygulamanin uzerine guncellemez. **Mevcut uygulamani silme.** Once PANDORA 10'daki *Not yedegi* dugmesi ile paylasilabilir yedek olusturulabilir (bunu yapmak icin PANDORA10'un Code on the Go'da mevcut uygulamanin ustune kurulabilmesi gerekir). Ustune kurulamiyorsa mevcut notlari manuel/kopyalayarak yedeklemek gerekir. Yeni imzali uygulamaya gecis ilk seferde yeniden kurulum gerektirebilir. Guncellemelerin sonsuza dek sorunsuz devam etmesi icin AYNI release keystore korunmalidir.

**APK'yi sessizce veya uzaktan telefona kurma:** Android kullanici onayi gerektirir. Bu surumde uygulama ici otomatik guncelleme denetimi henuz yok; buradaki ilk kilometre tasi bulutta otomatik APK **olusturma** altyapisidir.

Bu ortamda Android SDK olmadigindan workflow gercek GitHub runner uzerinde henuz calistirilamadi. GitHub baglandiktan sonra basarisiz derleme varsa loga gore duzeltilir.


## PANDORA 11 – deneysel Hey Pandora ve uyarı testi

Uygulama menüsündeki **Hey Pandora** düğmesi açık rızayla mikrofon ön plan hizmeti başlatır. Dinleme kalıcı sistem bildiriminden durdurulabilir. Bu bir kesintisiz hotword motoru değildir; sistem SpeechRecognizer çevrimdışı tercihine rağmen bulut kullanabilir ve başka uygulamalarda/çağrılarda kesilebilir. Algılanan uyandırma sözcüğü yalnızca bildirim gönderir; henüz serbest komut icra etmez. **Uyarı testi** bildirimi ve kulaklık bağlıysa (çağrı dışındayken) TTS sesli uyarısını dener. Otomatik Binance takibi henüz yoktur. PANDORA TEST sürümü eski kurulumdan ayrı veriler tutar. Kalıcı release APK imzası henüz kurulmamıştır.


### PANDORA 11 kurulum notu

Önceki PANDORA TEST debug APK'sı farklı GitHub çalıştırıcılarında farklı anahtarlarla imzalanabilir. PANDORA 11 deneysel sürümü `com.jarvis.kisisel.trial11` olarak **ayrı** kurulacak; kurulu PANDORA/PANDORA TEST verileri kaldırılmamalıdır. Bu bir kalıcı güncelleme çözümü değildir. Sonraki sürümler için sabit, güvenli release imzalama anahtarını GitHub Secrets ile kurmak gerekir. Gizli imzalama anahtarını repoya commit etmeyin.
