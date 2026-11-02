# PANDORA 21 · Hermes birleşik sürüm

PANDORA 17'nin sade monokrom sohbeti, beyaz dalga simgesi ve yerel Vosk uyandırması; PANDORA 20'nin Hermes Gateway bağlantısı ve ses tercihleriyle birleşir.

## Kurulum

- Kalıcı imzalı `com.jarvis.kisisel.stable` paketi PANDORA 17'nin üzerine güncellenir. Uygulamayı kaldırmadan yükle; mevcut not ve geçmiş verileri veritabanı yükseltmesiyle korunur.
- PANDORA 19/20 deneme paketleri ayrı Android uygulamalarıdır. Android Keystore anahtarları aralarında taşınmaz. Hermes anahtarını Menü → Hermes bağlantısı içinde bir kez girmen gerekir. Anahtarı sohbete gönderme.
- Mevcut Termux Hermes kurulumu ve `http://127.0.0.1:8642` adresi korunur. Termux'ta `hermes gateway` açık olmalı. Uygulama Termux'u uzaktan başlatamaz.
- Menü → Hermes bağlantısı → Bağlantıyı kontrol et, anahtarlı `/v1/models` kontrolü yapar. Anahtar kaydetmek hiçbir zaman "bağlandı" olarak gösterilmez.
- Ses için kulaklık ve mikrofon izni gerekir. Mikrofon düğmesi sessiz Vosk oturumunu açar. Oturum kapansa da Hey Pandora hizmeti açık kalır; menüden veya kalıcı bildirimden durdurulur. Kulaklık ayrılırsa hizmet kapanır; yeniden bağladıktan sonra yeniden başlatılır.
- Ekran için erişilebilirlik, görsel analiz izni ve Hermes/bulut modu gerekir. Diğer uygulamadayken Hey Pandora veya hızlı ayarlar kutucuğunu kullan. Hermes tarafında görsel destekli sağlayıcı gerekir; görüntü kabul edilmezse hata gösterilir, gizlice başka sağlayıcıya geçilmez.

## Bu sürüm

- Sistem konuşma tanıma penceresi yerine tek yerel mikrofon hattı; dinleme/yanıt durumları ve duyulan sözler için ayrı dalgalı ses ekranı.
- Türkçe ses seçimi ve ses tonu tüm yanıt yollarında uygulanır. Sesli yanıtlar yalnızca kulaklıkta; arama sırasında okunmaz.
- Yeni sohbet mevcut mesajları silmez. Eski konuşmalarda arama, sayfalama ve Android dosya seçicisiyle metin dışa aktarma.
- Hermes sohbet, sesli sorular ve görsel ekran yorumu. Yerel not/saat gibi görevler model istemez.
- Mesaj isteği alıcı seçimine ve düzenlenebilir SMS taslağına götürür. Gönderildiği iddia edilmez. Görüntülü arama seçenekleri yalnızca görünen uygun çağrı kontrolünü dener.
- İstek durdurma, HTTP yönlendirme engeli, sınırlı yanıt boyutu, hata/null yanıt ve Hermes terminal durum kontrolleri.

## Gerçek sınırlar

Hermes Android sistem yetkisi sağlamaz. Başka uygulamaları tamamen yönetme, normal aramayı başka uygulamada aynı kişiye otomatik aktarma, konuşma sırasında garanti mikrofon erişimi, sürekli proaktif hesap takibi ve Windows kontrolü bu APK ile tamamlanmış değildir. Sunucunun araçları kendi yapılandırmasına bağlıdır; ekran için salt bilgi talimatı istem düzeyindedir, sunucu araç izinlerinin yerini tutmaz.

İstemcide durdurma HTTP beklemesini ve sesli yanıtı durdurur; Hermes'te başlamış bir işlemin geri alındığını veya sunucuda durduğunu kanıtlamaz. Otomatik tekrar gönderim yoktur. Hermes'in kullandığı model ücretli olabilir; uygulama mevcut sağlayıcını değiştirmez.

## Doğrulama

GitHub Actions: saf Java komut/politika testleri; yerel sahte Hermes sunucusunda kimlik doğrulama, Türkçe yanıt, görüntü yükü, iptal, yönlendirme, null ve hata regresyonları; Android derleme ve lint; kalıcı APK imzası kontrolü. Gerçek cihazdaki kulaklık, gürültü, kilit ekranı ve Termux bağlantısı bu ortamdan doğrulanamaz.
