# PEGASUS Stable 1.0.1 — Telefon sesli komut testi

Bu APK, Stable 1.0.0 sürümünün **üzerine güncelleme** olarak kurulmak üzere hazırlanmıştır. Uygulamayı silme. Paket kimliği: `ai.pegasus.nexus.stable`. Kalıcı imza, önceki Stable sürümüyle aynı olmalıdır.

## Önce
1. PEGASUS Stable 1.0.0 cihazda yüklüyse kaldırma.
2. PEGASUS Stable 1.0.1 APK'yı indir; **Güncelle** seçeneğiyle kur.
3. PEGASUS'u aç; yeni **🎙 Mikrofon izni** tuşuna dokun, Android mikrofon iznini ver.
4. Erişilebilirlik hizmetinin açık kaldığını kontrol et (Android yeni özellikte yeniden onay isteyebilir).
5. Hassas olmayan bir uygulamaya geç. P düğmesini aç; **Başlat** tuşuna bas.

## Sesli komutlar
Bir defa **🎙 Konuş** tuşuna bas, sonra tek bir komut söyle. Tekrar komut vermek için her seferinde yeniden Konuş'a bas.
- "Tara" — görünür erişilebilir öğeleri bulur.
- "Sonraki" veya "İleri" — sonraki öğeyi seçer.
- "Önceki" veya "Geri" — önceki öğeyi seçer.
- "Dokun" — seçili öğeye güvenlik kontrollerinden sonra dokunur.
- "Kaydır" veya "Aşağı kaydır" — sayfayı kaydırır.
- "Dur" — ekran işlemlerini durdurur ve mikrofon oturumunu kapatır.

Testi şu sırada yap: **Tara → Sonraki → Dokun → Tara → Kaydır → Dur**. Her komut için ayrı Konuş basışı gerekir; Dokun işleminden sonra yeni tarama zorunludur.

## Bilinen sınırlar
- Bu bir **telefon üzerinde henüz doğrulanmamış test sürümüdür**.
- Ses tanıma Android cihazın kurulu konuşma tanıma hizmetine bağlıdır; çevrimdışı çalışması garanti değildir. Servis sağlayıcısı sesi ağ üzerinden işleyebilir.
- Android bazı arka plan mikrofon oturumlarını engelleyebilir. Hata kodunu ve ekranı raporla; özellik çalışıyor varsayılmamalıdır.
- "Hey Pegasus", sürekli dinleme, Gemini Live, otomatik akıl yürütme yok.
- Finans, ödemeler, mesaj gönderme, sistem izinleri ve kimlik bilgileri ekranlarında test yapma.
- Acil durumda **DUR** düğmesine bas veya Android Ayarları'ndan erişilebilirlik hizmetini kapat.

## Kurulum geçerlilik kapısı
- Aynı `applicationId`, aynı imza sertifikası ve sürüm kodunun 500'den 501'e yükselmesi otomatik denetlenir.
- 502 sürümü ayrıca aynı imzayla derlenerek sonraki güncelleme uyumluluğu denetlenir.
- APK derlemesi güvenilir kullanıcı testi yerine geçmez.
