# PANDORA · Hermes

Monokrom Android kişisel asistan. Bu dal, PANDORA 17'nin yerel Vosk uyandırma ve görsel yorumlama altyapısını PANDORA 20'nin mevcut Hermes bağlantısıyla birleştirir.

Kurulum, sürüm farkları ve doğrulama sınırları: [PANDORA21.md](PANDORA21.md).

- Hermes Gateway: `http://127.0.0.1:8642`; anahtar Android Keystore ile saklanır.
- Kulaklıkla sessiz Hey Pandora, dalgalı ses ekranı, kalıcı sohbet geçmişi.
- Ekran görüntüsü açık kullanıcı isteği ve ayrı görsel analiz izniyle seçili sağlayıcıya gönderilir.
- Kararlı Android paketi: `com.jarvis.kisisel.stable`; deneme paketleri ayrı kalır.
- Modelleri hazırlama: `python3 scripts/prepare_voice_model.py`.
- Testler: `bash tests/RUN_TESTS.sh`, `gradle :app:testDebugUnitTest :app:lintDebug`.
- İmzalı APK: PANDORA Stable workflow. Gizli anahtarları ve keystore'u kaynak koduna eklemeyin.
