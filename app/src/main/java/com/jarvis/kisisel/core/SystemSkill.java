package com.jarvis.kisisel.core;
import android.content.Context;
import java.util.function.Consumer;
public final class SystemSkill implements PandoraSkill {
 public String name(){return "Sistem";}
 public String description(){return "Komut, kapasite ve görev durumunu açıklar";}
 public boolean handle(Context c,String q,Consumer<String> reply){
    if(q.equals("pandora durum")||q.equals("durum")||q.contains("hangi ajanlar")||q.contains("hangi modüller")){
      reply.accept("PANDORA modüler sistemi çalışıyor. Sesli komut, not, hatırlatıcı, yerel ve isteğe bağlı ücretsiz bulut sohbeti kullanılabilir. Binance, WhatsApp kontrolü, arka planda sürekli ses dinleme ve video yayınlama henüz bağlı değil.");
      return true;
    }
    return false;
 }
}
