# PANDORA

**Ozel, siyah-beyaz Android yapay zeka asistani.**

Bu depo kullaniciya ait ozel kaynak kodu ve otomatik APK derleme sistemidir.

## Ilk kurulum (yalnizca bir kez)

1. [Dosya yukleme sayfasi](https://github.com/sorvaxllc-code/PANDORA/upload/main) uzerinden `PANDORA 10.zip` dosyasini ana dizine yukleyin.
2. Dosya `main` dalina kaydedilince [GitHub Actions](https://github.com/sorvaxllc-code/PANDORA/actions/workflows/apk.yml) otomatik calisir.
3. Action, ZIP'i acip `app/`, `tests/` ve Gradle dosyalarini bu depoya ekler.
4. Java testleri ve Android derlemesi basarili olursa `PANDORA-APK` adiyla indirilebilir *debug APK* olusturulur.

Sonraki kod guncellemeleri bu depodaki kaynak dosyalarina dogrudan yapilir, GitHub Actions yeniden APK derler. Kullanici her seferinde Code on the Go'yu calistirmak zorunda kalmaz.

## Guvenlik ve imzalama

- Repo **Private** olmali. API anahtarlari, .jks keystore ve sifreler buraya yuklenmez.
- Debug APK, telefonda kurulu Code on the Go surumuyle ayni imzaya sahip olmayabilir. Bu durumda Android eski surumun uzerine kurulum yapmaz.
- Notlarinizi yedeklemeden eski PANDORA uygulamasini kaldirmayin.
- Gercek guncelleme kanali icin kalici ve guvenli release imzalama anahtari ile uygulama ici guncelleme bildirimi ayri bir gelistirme adimidir.
- Burada kurulan is akisi otomatik **APK olusturur**, telefona sessizce yuklemez.

## Proje plani

Moduler ajan sistemi, yerel/bulut model, sesli uyandirma, proaktif bildirimler, guvenli cihaz kontrolleri, e-ticaret ve video uretimi modulleri zamanla gelistirilecektir.
