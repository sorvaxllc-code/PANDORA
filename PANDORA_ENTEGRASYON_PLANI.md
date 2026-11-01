# PANDORA — Tek uygulama ve hazır bileşen entegrasyon planı

> Bu belge gelecek geliştirmeler için tekniktir. Aşağıdaki dış bağımlılıklar şu anda PANDORA'ya eklenmiş **değildir**.
> Yeni APK göndermek yerine önce mevcut kaynak üzerinde birleştirme, test ve tek kalıcı release üretme esastır.

## Kalıcı uygulama kimliği / güncelleme

- Üretim paket kimliği: `com.jarvis.kisisel.stable`. **Değiştirilmeyecek.**
- İmza: sadece GitHub Actions secrets içindeki mevcut kalıcı PKCS12 anahtar, alias `pandora`; yeniden üretilmeyecek.
- Yeni sürümde `versionCode` artırılır. Android, aynı paket ve aynı imzayla mevcut kurulumun üstüne güncelleme yapar; sil-yükle gerekmez.
- GitHub Actions yalnızca **bir** indirilebilir sürüm üretir: `PANDORA` isimli imzalı release APK artifact'ı.
- Testler CI içinde yapılır; `.trialX` deneme APK'sı kullanıcıya dağıtılmaz.
- Android'de kullanıcı izni olmadan APK otomatik kurulamaz; indirme/kurma onayı kullanıcıda kalır. Özel GitHub deposu kimlik doğrulaması ister.
- Mevcut PANDORA 11 / deneme uygulamalarını otomatik silme ve kullanıcı verisini otomatik taşıma yoktur.
- Uygulama notlarının/verisinin sürüm güncellemesinde korunması için aynı paket kimliği şarttır.

## Mevcut çalışır modüller

- Android arayüzü, basit yerel not ve hatırlatıcı, bildirim, sesle komut, deneysel ekran-kapalı uyanma.
- Google/Android sistem konuşma tanıma ve sistem TextToSpeech (şimdilik). Arka planda çevrimdışı çalışma **garanti değil**.
- Sesli cevap tercihine göre yalnızca bağlı kulaklıktan; kulaklıksız bildirim. Telefon görüşmesini bölme.
- Yerel llama.cpp HTTP sağlayıcı ve kullanıcı iznine tabi bulut sağlayıcı arayüzleri; 8B modelin bu telefonda yavaşlığı biliniyor.
- Basit `AgentRegistry` modül arayüzü, gerçek çoklu ajan otomasyonu değil.

## İncelenecek açık kaynak bileşenler (henüz entegre edilmedi)

| İhtiyaç | Öneri | Lisans / dikkat |
|---|---|---|
| Android'de gerçek yerel uyandırma kelimesi, VAD, STT, isteğe bağlı TTS | https://github.com/k2-fsa/sherpa-onnx | Apache-2.0 kod; Android örnekleri var. Uygun **Türkçe model**, model lisansı, CPU/RAM/batarya testi ayrı gerekli. |
| Alternatif uyandırma motoru | https://github.com/dscripka/openWakeWord | Apache-2.0 kod; **hazır modeller CC BY-NC-SA 4.0**, ticari kullanım kısıtı var ve Python/Android taşıması gerektirebilir. Varsayılan seçim değil. |
| Yerel LLM | https://github.com/ggml-org/llama.cpp | Mevcut Termux HTTP sunucusu ile kullanım; APK'ya model gömmek gerekmiyor. Önce hafif model ve telefon performansı ölçülür. |
| Hatırlatma / izin / bildirim / TTS | Android yerleşik API'leri | Üçüncü taraf repo gereksizse mevcut platform altyapısı korunur. |

## Bütünleşik mimari

`PANDORA UI → Wake/VoiceAdapter → IntentRouter → AgentRegistry → Onay/İzin Katmanı → Android veya ModelRouter → Yanıt → Kulaklık TTS ya da Bildirim`

- Tek Android projesi, tekrar kullanılan modüler kod; ayrı test uygulaması yok.
- Her harici kütüphanenin bağımlılık, mimari uyumluluk, lisans ve model kaynakları denetlenir.
- Her modül temel kaynak testlerinden ve GitHub release derlemesinden geçmeden üretim moduna girmez.
- Hesap bağlama, alım-satım, ödeme, medya yayınlama, WhatsApp eylemleri gibi etkili işlemler için ayrı kullanıcı onayı gerekir.
- Önce güvenilir arka plan uyandırma ve ses deneyimi; sonra hibrit model, Windows köprüsü, kripto read-only, ticaret ve video araçları.
