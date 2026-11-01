# PANDORA 10 degisiklikleri

- `null` (bos/iceriksiz) OpenRouter yanitini basarili sohbet mesaji olarak gostermeme; 200 durum kodlu API hata cevabini kontrol etme.
- Bulut yanit token ust sinirini 240'tan 768'e cikarma (reasoning-only yanit ihtimalini azaltmaya yonelik, kesin cozum garantisi yok).
- Android uzerinden kullanici onayli metin not yedegi paylasma.
- GitHub Actions: guncellemede debug APK, `v*` tag ile gerekli gizli imzalama bilgileri var ise imzali release APK/GitHub Release.
- Proje surumu: 1.0.0 (versionCode 10).

Yalnizca kod/test incelemesi yapildi; GitHub/Android APK yapimi henuz gercek cihazda dogrulanmadi.
