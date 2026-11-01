# PANDORA 17 — İlk birleşik geliştirme sürümü

## Uygulananlar
- Sade siyah-beyaz sohbet görünümü; notlar ve kurulum kontrolleri menüde.
- Cihaz üzerinde Vosk ile ayrı çağrı ifadesi ve Türkçe komut algılama.
- Yalnızca Hey Pandora çağrısıyla komut penceresi; ekranda 1,8 saniyelik beyaz dalgalı simge.
- Ekranın piksellerini, kullanıcının sorusuyla birlikte görsel modele ileten analiz yolu. Yeni ve ayrı ekran görüntüsü bulut izni zorunlu.
- Arka plan yanıtlarının yerel sohbet geçmişine kaydı; otomatik 150 mesaj silme kaldırıldı. Arayüz son 150 mesajı gösterir.
- Açık not alma komutları yerel çalışır. Mesaj ve görüntülü görüşme için ortak görev giriş noktası.

## Henüz tamamlanmayanlar
- Her spontane isteği araçlara bölen genel görev planlayıcısı.
- Sesle kişi çözümleme ve doğrudan mesaj gönderimi. Bu sürüm kişi seçici ve mesaj taslağı açar; kullanıcı mesajı uygulamasında gönderir.
- Normal aramayı aynı kişiyle başka uygulamaya otomatik taşıma. Mevcut görüşmedeki görünür video düğmesini deneyebilir; geçişin tamamlandığını iddia etmez.
- Arama sırasında Hey Pandora: Android mikrofon erişimi nedeniyle garanti edilmez; fiziksel cihazda denenmedi.
- Bekle/devam ile konuşmayı kesme; Bluetooth mikrofonunu özel olarak seçme; kilitli ekran ve üretici pil kısıtlamalarında dayanıklılık.
- Önizlemedeki bağımsız tam ekran sesli görüşme görünümü. Dalgalı simge arka plan overlay'i olarak uygulanmıştır.
- Windows, proaktif olay izleme, ticaret/içerik otomasyonları.

## Doğrulama
- Saf Java testleri, Android derleme ve lint: geçti (7262750).
- Aynı kalıcı CI anahtarıyla imzalı APK üretimi ve imza doğrulaması: geçti (7262750).
- Vosk sözlük kontrolü: Türkçe model Pandora sözcüğünü içermediği için ayrı uyandırma modeli eklendi.
- Sentetik İngilizce ses örnekleri: Hey Pandora tetiklendi; üç sıradan konuşma örneği tetiklemedi. Bu, Türkçe aksan veya makine gürültüsü testi değildir.
- 194f75c: komut verilmeden zaman aşımı olursa sonraki çağrıda simgenin yeniden gösterilmesi düzeltildi; CI sonucu PR'da takip edilir.
- Fiziksel Android/ Bluetooth, gerçek bulut API yanıtı ve görsel arayüz cihaz testi yapılmadı.

## Kurulum
Mevcut PANDORA'yı silmeden imzalı APK ile güncelle. Menüden Sesle uyandırma ve Etkinleşme simgesi izinlerini aç. Görsel yorumlama için Ekran erişimi ve Görsel analiz izni açılmalı; bulut/otomatik modda mevcut OpenRouter anahtarı kullanılır. Şifreler ve hesap ayarları silinmez. Android'in zorunlu mikrofon/servis göstergeleri kalır.

Yerel modeller pakete dahil olduğundan APK yaklaşık 165 MB'dır; ilk açılışta modeller açılırken beklemek gerekebilir. Ücretsiz bulut modelinin erişimi ve kotası sağlayıcıya bağlıdır.
