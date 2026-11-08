# PEGASUS Stable 1.0.6 — Gemini Live ile güvenli telefon araçları

## Neler eklendi?
- Live API'de açıkça tanımlanmış 7 işlevin `toolCall` istekleri Android'de karşılanır; sonucu `toolResponse` olarak Live görüşmesine döner.
- Bu oturumda **Telefon araçlarına izin ver** düğmesine basılmadan hiçbir araç çalışmaz (acil DUR hariç).
- Ekran kontrolü için PEGASUS'un erişilebilirlik hizmetinin açık olması ve P panelinden **Başlat** yapılması gerekir.
- Test kapsamı **yalnızca PEGASUS'un kendi arayüzü ve izinli hesap makinesi paketleri** ile sınırlıdır. Diğer uygulamalar, finans, mesajlar, ödeme, sistem izinleri ve genel ekran içerikleri bu sürümde desteklenmez.
- **Tara:** En fazla 8 kısa, hassas olmayan düğme etiketi kullanıcının bu oturumdaki izninden sonra Gemini'ye gönderilebilir. Bu etiketler güvenilmeyen dış veridir.
- **Sonraki / Önceki / Kaydır:** Daha önce test edilmiş erişilebilirlik yöntemleri kullanılır.
- **Dokun:** Model yalnızca önerir; native Android onay penceresi açılır. İptal ve ret sonucunda hiçbir dokunma gerçekleşmez. Onaydan sonra hedefin güncelliği ve politikası yeniden kontrol edilir.
- **Android Ayarlar'ı aç:** Açabilir ancak PEGASUS arka plana geçeceğinden Live ses oturumu kasıtlı olarak kapanır.
- **DUR:** Live oturumunu ve ekran eylemlerini keser.

## Gerçek cihaz testi
1. Eski PEGASUS Stable'ı kaldırmadan 1.0.6 APK dosyasına dokunup **Güncelle** yap.
2. Uygulamayı aç; mevcut Gemini anahtarının korunduğunu doğrula.
3. Erişilebilirlik hizmeti ve P paneli çalışıyor mu bak. **Başlat** tuşuna bas.
4. PEGASUS > Ayarlar > Gemini Live bölümündeki **Telefon araçlarına izin ver** tuşuna basıp Android onayını oku ve açıkça kabul et.
5. **Gemini Live'a bağlan**. "PEGASUS, bu ekranı tara" de. Önceki Gemini sesi bozulmamalı.
6. "Sonraki öğeyi seç" ve "Aşağı kaydır" de. P paneli ve Gemini'nin işlev sonucunu doğrula.
7. "Seçili öğeye dokun" de. Native onay penceresi çıkmalı; ilkinde **Reddet** ile hiç dokunma olmadığını kontrol et. Ardından güvenli bir düğme için **Dokun** onayını test et.
8. "Android Ayarlar'ı aç" de. Ayarlar açılmalı; **Gemini ses oturumu kapanır**. Bu, henüz çoklu uygulama arka plan ses kontrolü değildir.
9. "DUR" ile durdurmayı dene. Eylem izinleri oturum dışına taşınmamalı.

## Sınırlar
- "Hey PEGASUS" arka planda dinleme, canlı telefon uygulamaları arasında kesintisiz sesli kontrol ve otomatik ajanlar **henüz yok**.
- Modelin söyledikleri doğrudan Android işlemi haline getirilmez; yalnızca adı tanımlı araç çağrıları kabul edilir.
- Google API anahtarı yalnızca Keystore'da şifrelenmiş olarak cihazda tutulur; APK ve GitHub'a yazılmaz. Üretim için kimlik doğrulamalı sunucu + geçici token önerilir.
- Erişilebilirlik hizmeti Android'in kullanıcı tarafından açılan bir sistem iznidir; kullanıcı kapatabilir.
- CI, kodun derlenmesini / kalıcı paket kimliği ve imzayı denetler. Gerçek telefon testini kullanıcının yapması gerekir.

Gemini Live araç protokolü: https://ai.google.dev/gemini-api/docs/live-api/tools
WebSocket toolCall/toolResponse: https://ai.google.dev/api/live
