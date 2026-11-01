## PANDORA 8 durum
- Hafıza ve temel sesli komut (önceki sürümde cihazda denendi)
- Ücretsiz bulut bağlantısı ve anahtar test UI eklendi; henüz cihaz/gerçek anahtarla doğrulanmadı
- Sonraki öncelik: kullanıcı testinden sonra konuşma hızı, modüler görevler, arka plan uyandırma (Android sınırlarıyla)

# PANDORA — Genel proje gereksinimleri ve geliştirme planı

**Görsel tasarım:** Siyah-beyaz, yüksek kontrast, gereksiz renk yok. Uygulama adı PANDORA. Yeni beceriler yeni modüllerle eklenir.

**Bu belgedeki hedefler, şu anda çalışan özellikler anlamına gelmez.** Uygulama durumu aşağıdaki sütunda gösterilir.

| Kullanım senaryosu | Hedef davranış | v0.5 durumu |
|---|---|---|
| Türkçe konuşma / sesli cevap | PANDORA ile konuşma | Mikrofona basınca komut, TTS var |
| Kişisel hafıza | Yerel not ve konuşma geçmişi, izinli bulut | Yerel hafıza var, bulut hafıza onaylı |
| Hibrit akıl | Yerel Qwen, ücretsiz bulut, otomatik geçiş | Kaynak kodda var, APK'da henüz test edilmedi |
| 'Hey Pandora' uyandırma | Telefon kilitliyken / ekranda başka uygulama varken | Plan, uygulama henüz yok |
| Proaktif kritik uyarı | Kulaklık bağlıysa sesli, değilse bildirim | Hatırlatıcı bildirimleri mevcut; olay sistemi plan |
| WhatsApp görüşmesi | Sesliden görüntülüye geçiş, görüşmeyi bitirme | Plan, platform izin ve erişilebilirlik testi gerekli |
| Video-dizi yapımı | Senaryo, görsel, ses, montaj, yayına hazırlık | Plan |
| YouTube/TikTok yayınlama | Zamanlı plan, platform izinleriyle paylaşma | Plan; platform API inceleme / yetki şart |
| Binance/BingX | Sadece okuma ile bakiye/pozisyon, analiz, risk uyarısı | Şu an hesap erişimi YOK, sahte rakam üretilmez |
| Kripto emir gönderme | Ancak açık, işlem bazlı onay ve sıkı limitlerle | Varsayılan YASAK |
| Shopify/e-ticaret | Araştırma, SEO, ürün, reklam, organik içerik | Plan |
| Telefon/Windows kontrolü | Onaylı uygulama açma, cihaz köprüsü | Telefonda sınırlı komut var; Windows bağlantısı plan |
| Güvenlik | Dosya analizi, uyarı, karantina, günlük kontrol | Plan, mevcut işletim sistemi korumaları kaldırılmayacak |
| Çoklu ajanlar | Görevleri alt ajanlara bölme, iş günlüğü | Ajan arayüzü ve kayıt sistemi temel düzeyde var |

## Mimari taslak

- **PANDORA arayüzü**: sohbet, mikrofon, yanıt, bildirimler, ayarlar.
- **AI Router**: ücretsiz bulut kullanımı, model seçimi, yerel fallback, kota hatası ve bağlam gizliliği.
- **SkillRegistry**: sistem, borsa(okuma), içerik üretimi, e-ticaret, güvenlik ve cihaz ajanlarını modül olarak kaydetme.
- **Workflow Scheduler**: günlük/üç günde bir video taslağı, risk kontrolü, yayın ve hatırlatıcılar. Android zamanlayıcısı tek başına aralıksız sunucu değildir; gerektiğinde güvenilir backend gerekir.
- **Capability Manager**: her araca yalnızca gereken yetki; ödeme, işlem, veri silme ve dışa paylaşma öncesinde açık onay; denetim kaydı.
- **Windows Companion**: daha ağır iş, güvenli dosya taraması, tarayıcı otomasyonu ve üretim işlerini PC tarafına taşıma.
- **Notification Agent**: bağlı kulaklık durumuna göre uyarı; hassas bildirim içeriklerini kilit ekranında gizleme.

## Güvenlik ilkeleri

1. Özel dosyalar, konuşma geçmişi ve notlar varsayılan olarak cihaz dışına gönderilmez; geçerli kullanıcı onayı gerekir.
2. API anahtarları yazışmalara ve kaynak koduna konulmaz; şifreli saklanır.
3. Sisteme sınırsız kök/root yetkisi verme yerine izin sınırı ve denetim kullanılır.
4. Finansal işlemler, ödemeler, uygulama kaldırma, güvenlik izolasyonu ve silme kararları ayrı onaya tabidir.
5. Otomatik bulut geçişi yalnızca kullanıcının izin verdiği ve ücretsiz kontenjanı olan modellere yapılır. Bedava bulut sürekli erişilebilir değildir.
6. Android arka plan mikrofonu ve WhatsApp görüşmeleri, üçüncü taraf uygulama/Android yetkilerinin sınırları içinde kalır.

## Sonraki sürümün birinci önceliği

PANDORA'yı seçilebilir varsayılan sesli asistan rolüne hazırlama; arka plan 'Hey Pandora' tetiklemesinin Android ve cihaz sürümünde neleri desteklediğini deneme; Bluetooth kulaklığa düşük gecikmeli TTS uyarıları; uyarı önem düzeyleri. **Bu özellikler v0.5'e dahil değildir.**

## v0.5 temel taşı
Model yalnızca izinli bir `PANDORA_ACTION_V1` JSON eylemi üretebilir; uygulama gelen komutu sınırlar. Bu ilk adım, daha sonra her ajan için ayrı izin, onay, denetim ve geri alma mekanizmalarına genişletilecek. Sürekli dinleme ve proaktif bildirim ajanları hâlâ sonraki geliştirme işidir.

## v0.6 Kararlılık iyileştirmeleri
Yerel modelde büyük gecikme görüldüğü için açık hafıza komutları modelden ayrıldı, istek başına durdurma/iptal eklendi, geç gelen yanıtların eylem yapması engellendi. Kısa vadeli bir sonraki ana görev: güvenli, ücretsiz bulut modelini kullanıcı opt-in ile bağlamak; uzun vadeli: sürekli asistan ve uzman ajanlar.


### PANDORA 9
Bulut sohbet yönlendirmesi düzeltildi; gerçek bulut yanıt testi ve yanıt kaynağı göstergesi eklendi.
