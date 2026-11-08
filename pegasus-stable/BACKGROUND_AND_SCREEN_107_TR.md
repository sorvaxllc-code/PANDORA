# PEGASUS Stable 1.0.7 — Arka plan sesi + anlam odaklı ekran komutları

## Sorun ve düzeltme
1. **Uygulamadan çıkınca Gemini kapanması:** 1.0.6'da Gemini bağlantısı MainActivity yaşam döngüsündeydi ve `onPause()` içinde kapanıyordu. 1.0.7 bağlantıyı **PegasusLiveService** isimli Android mikrofon foreground service içine taşır; kullanıcı PEGASUS içinden bir kez başlatır. Başka uygulamaya geçildiğinde oturum açık kalabilir.
2. **Her seferinde Başlat zorunluluğu:** Oturum başlatılınca telefonda kalıcı bildirim görünür. Bildirimde **DUR** ile mikrofon ve araç erişimi kapanır. Android pili/belleği/üretici kuralları, ağ kopması veya servis kısıtları nedeniyle sonsuz kesintisizlik garanti edilmez. Kısmi bağlantı kopmaları için sınırlı tekrar deneme ve Gemini oturum resumption ayarları eklenmiştir.
3. **P penceresine bağımlılık:** P penceresi **Gizle/Göster** komutuyla isteğe bağlıdır. Araçlara bir kez izin vermek erişilebilirlik kontrolünü P panelindeki Başlat'a bağımlı olmadan hazırlar.
4. **“Tara” yalnızca düğme tarıyor:** Komut niyetine göre farklı Live işlevleri:
   - `pegasus_scan` — tıklanabilir düğmeleri bulur.
   - `pegasus_read_screen` — görünür erişilebilir ekran metnini okuyup açıklar.
   - `pegasus_translate_screen` — ekran metnini Gemini'ye gönderir ve Türkçe çeviriyi ister.
   - Belirsiz komutta Gemini açıklayıcı soru sormalıdır.
5. **Güvenlik:** İlk aşamada metin okuma yalnızca PEGASUS, bazı hesap makinesi uygulamaları ve Wikipedia uygulamasını destekler. Şifre alanları, düzenlenebilir alanlar, hassas metinler, uzun sayılar/e-postalar çıkarılır; tarayıcı, banka, mesaj, kimlik ve sistem izinleri uygulamalarında metin okuma engellenir. Görsel piksel tarama ve OCR **henüz yoktur**.

## Test sırası
1. PEGASUS 1.0.6'yı **silme**. 1.0.7 APK dosyasına dokun ve **Güncelle** seç.
2. Android bildirim izni ve mikrofon izni onaylarını kontrol et.
3. PEGASUS > Ayarlar > Gemini Live bölümündeki **Sürekli ses oturumunu başlat** düğmesine bas. Android'in bildirim panelinde PEGASUS aktif hizmeti görünmeli.
4. **Telefon araçlarına izin ver** düğmesine dokun; modelin bu oturumda ekrandaki izinli içerikleri Google'a gönderebileceği açıklamasını onayla.
5. **P penceresini gizle** düğmesine bas.
6. Ev ekranına veya hesap makinesine geç. PEGASUS bildiriminin açık kaldığını ve sesli görüşmenin kesilmediğini dene: **"PEGASUS, beni duyuyor musun?"**
7. Hesap makinesindeyken **"Butonları tara"**, sonra **"Sonraki düğmeye geç"** komutları.
8. PEGASUS ekranına veya desteklenen Wikipedia uygulamasına geç ve **"Ekranda ne yazıyor?"**, ardından **"Bu yazıyı Türkçeye çevir"** de. Görünür erişilebilir metin yoksa açıkça hata vermeli.
9. Bildirimdeki **DUR** ile mikrofonu ve işlem iznini kapat. Mikrofon göstergesi kaybolmalı.

## Bilinen sınırlar
- **Bu sürüm gerçek yerel 'Hey PEGASUS' uyandırma motoru değildir.** Mikrofon, kullanıcı açık oturumu başlattığında aktif kalır ve ses verisi Gemini API'ye akar. Bekleme/yalnızca uyandırma ifadesini yerelde dinleme ileriki ayrı çalışmadır. Sürekli ses gönderimi **veri kullanımı, API kotası/ücreti ve pil tüketimi** doğurabilir.
- Bir mikrofon foreground service, Android'in her durumda durmayacağı veya yeniden başlatacağı kalıcı bir sistem asistanı anlamına gelmez. Android 14+ arka plandan yeni mikrofon hizmeti başlatmayı genellikle engeller; hizmeti uygulama görünürken kullanıcı başlatmalıdır.
- Telefon yeniden başladığında mikrofon otomatik başlamaz. Yeniden açmak için kullanıcının uygulamaya gelmesi gerekir.
- Başka uygulamalara sesli komut verebilmek, bu uygulamalarda sınırsız ekran okuma veya otomatik işlem yapabilmek anlamına gelmez. Önceki hassas eylemler için ayrıca Android onayı sürer.
- Gemini erişim süresi ve ağ bağlantısı kesilebilir. Sınırlı yeniden bağlantı denemeleri vardır; %100 24/7 garanti edilemez.
- Ekran taramasında `AccessibilityNodeInfo.text` kullanılıyor. Canvas, video, PDF görüntüsü veya OCR gerektiren ekranların tamamı okunamaz. Ekran görüntüsüyle görsel analiz sonraki adım, ayrı izin ve gizlilik kontrolleri gerektirir.
- Mevcut kullanıcı API anahtarı yeniden istenmez; yerel Android Keystore koruması sürer. Üretim düzeyinde güvenlik için sunucuda yönetilen geçici oturum anahtarına geçilmesi önerilir.

## Sürüm ve güncellemeler
- Paket kimliği: `ai.pegasus.nexus.stable`
- 1.0.6: versionCode 506
- 1.0.7: versionCode 507
- Yayın, yalnızca aynı üretim sertifikasıyla APK imzası doğrulandığında ve sonrasında 508 deneme güncellemesi başarıyla derlendiğinde yapılır.
- CI derlemesi **cihazın Android izinleri, gerçek Gemini ses kalitesi veya kesintisiz arka plan davranışını kanıtlamaz**.

Resmî Android kaynakları:
https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
https://developer.android.com/develop/background-work/services/fgs/changes

Gemini oturum yönetimi:
https://ai.google.dev/gemini-api/docs/live-api/session-management
