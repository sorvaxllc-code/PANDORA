# PEGASUS Stable 1.0.11 — Ekrana erişemiyorum düzeltmesi ve tanı

## Neden?
1. **3B P hologramı** `SYSTEM_ALERT_WINDOW` özel iznini kullanır. Bu izin, ekranda şeffaf bir simge göstermeye yarar; uygulama metinlerini okumaya yetmez.
2. **Android erişilebilirlik hizmeti** PEGASUS içinde ayrı etkinleştirilmelidir. Android 13/üretici yazılımında APK kaynağından yüklenen uygulamalarda kısıtlı ayarlar uyarısı çıkabilir. Açılması yalnızca kullanıcının güvenli kararına bırakılır.
3. Gemini telefon araçları **ayrıca oturum bazında onay** ister. Arka plan ses oturumu açık olması, ekran araçlarının açık olduğu anlamına gelmez.
4. Android bazen `rootInActiveWindow` yerine erişilebilirlik overlay'ini veya boş değer döndürür. 1.0.11 önce **aktif/odaklı TYPE_APPLICATION penceresini** seçerek hata durumlarını ayırır.
5. 1.0.8'den gelen tek oturumluk uygulama izin penceresinin kendisi ekran odağını değiştirebiliyordu; izin kabulü artık açıkça gösterilen **orijinal paket kimliğine** bağlıdır. Sonraki her okumada uygulamanın hâlâ önde olduğu ve korumalı olmadığı tekrar denetlenir.

## Test
1. Mevcut PEGASUS'u **silmeden** 1.0.11 APK'ya güncelle.
2. PEGASUS → Ayarlar → **Ekran erişimini kontrol et** düğmesine bas.
3. "Android erişilebilirlik hizmeti bağlı değil" çıkarsa Android Erişilebilirlik ayarlarından PEGASUS erişimini etkinleştir. Bu işlem **manuel** olmalıdır.
4. "Telefon araçları bu ses oturumunda kapalı" çıkarsa PEGASUS'ta **Telefon araçlarına izin ver** seçeneğini onayla.
5. Ses oturumunu başlat ve **Hesap Makinesi** uygulamasına git.
6. "PEGASUS, bu ekrandaki yazıları oku" de; önceki sürümlerden daha kesin hata veya metin cevabı gelmeli.
7. Başka normal bir uygulamada onay penceresi açılırsa **Bu oturumda izin ver** ile onayla, sonra aynı soruyu yeniden söyle.
8. Daha sonra bildirimden **DUR**: mikrofon kapanmalı, tek oturumluk uygulama izinleri silinmeli ve hareketli P hologramı kaybolmalı.

## Tanı kodları
- `A11Y_OFF`: Erişilebilirlik hizmeti bağlı değil.
- `TOOLS_OFF`: Ses oturumunda telefon araçlarına izin verilmedi.
- `SERVICE_PAUSED`: Ekran kontrolü DUR ile kapatıldı veya oturum başlamadı.
- `ANDROID_NO_WINDOW`: Android, odaklanmış uygulama ağacını vermiyor.
- `APP_NEEDS_CONSENT` / `PEGASUS_CONSENT`: Bu normal uygulama için tek oturumluk açık onay gerekli.
- `CONSENT_PENDING`: Ekrandaki izin penceresine yanıt verilmesi gerekiyor.
- `PEGASUS_PROTECTION`: Banka, parola, mesajlaşma, sistem izinleri veya başka korunan uygulama; güvenlik politikası.
- `NO_ACCESSIBLE_TEXT`: Uygulama, erişilebilir metin vermiyor. Video, çizim veya görüntüden metin okumak için ayrıca OCR/görsel analiz gerekli.
- `READY`: Aktif uygulamada erişilebilir metin okuma için gerekli yerel izinler hazır.

## Değişmeyenler
- NEXUS siyah-beyaz tema, 3B P hologramı, sistem üstü gösterim izni ve sürekli Gemini Live ses servisi.
- Paket kimliği `ai.pegasus.nexus.stable` ve kalıcı imza sertifikası.
- Android korumalarının aşılmaması; şifre, banka, mesajlaşma, ödeme ve sistem izin ekranlarının kapalı kalması.
- Otomatik tıklama/izin verme veya kullanıcının rızası olmadan görüntü gönderme **yoktur**.

Not: CI derleme testi, gerçek cihazda ses, erişilebilirlik ve izinlerin beklenen şekilde çalıştığını kanıtlamaz. Cihaz üzerindeki tanı butonu bu ayrımı görmek içindir.

Android geliştirici başvuruları:
https://developer.android.com/reference/android/accessibilityservice/AccessibilityService
https://developer.android.com/reference/android/accessibilityservice/AccessibilityServiceInfo#FLAG_RETRIEVE_INTERACTIVE_WINDOWS
