# PANDORA'ya yeni özellik nasıl eklenir?

PANDORA bir monolit olarak büyümemeli. Her yeni yetenek için önce hangi veri/yetkiye erişeceği belirlenir, sonra bir modül hazırlanır.

**Örnek:** `app/src/main/java/com/jarvis/kisisel/core/SaatSkill.java` dosyası:

```java
package com.jarvis.kisisel.core;

import android.content.Context;
import java.util.function.Consumer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class SaatSkill implements PandoraSkill {
    @Override public String name() { return "Saat"; }
    @Override public String description() { return "Telefonun saatini bildirir"; }
    @Override public boolean handle(Context ctx, String q, Consumer<String> reply) {
        if (!q.equals("saati söyle")) return false;
        reply.accept(new SimpleDateFormat("HH:mm", Locale.forLanguageTag("tr-TR")).format(new Date()));
        return true;
    }
}
```

Ardından `AgentRegistry.standard()` içine `.register(new SaatSkill())` ekle.

**Gerçek harici hesap modülleri** daha zor: yetki bağlantısı, yetki iptali, veri şifreleme, hata yönetimi, sınır/kota, onay ekranı, görev günlüğü ve testler gerektirir. Sadece sohbet modelinden metin istemek gerçek hesap erişimi sağlamaz.
