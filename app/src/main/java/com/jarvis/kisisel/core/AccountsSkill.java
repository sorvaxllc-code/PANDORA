package com.jarvis.kisisel.core;
import android.content.Context;
import java.util.function.Consumer;
/** Never claims access to unconnected accounts or performs trades. */
public final class AccountsSkill implements PandoraSkill {
 public String name(){return "Hesap erişim güvenliği";}
 public String description(){return "Bağlı olmayan hesaplarda sahte bakiye veya işlem göstermeyi önler";}
 public boolean handle(Context c,String q,Consumer<String> reply){
   if((q.contains("binance") || q.contains("bingx")) &&
      (q.contains("bakiy")||q.contains("pozisyon")||q.contains("long aç")||q.contains("short aç")||q.contains("emir ver"))){
       reply.accept("Henüz borsa hesabına güvenli, sadece okuma yetkili bir bağlantım yok. Gerçek bakiye veya pozisyon görmüyorum; senin adına emir de veremem. Bağlantı ve risk kontrolü modülünü sonraki sürümde ayrı ekleyeceğiz.");return true;
   }
   if(q.contains("whatsapp") && (q.contains("sonlandır")||q.contains("görüntülü"))){
       reply.accept("WhatsApp görüşmesine erişimim henüz yok. Sesli görüşmeyi doğrudan kapatamam veya görüntülüye çeviremem. Android ve WhatsApp izinleri doğrulandıktan sonra güvenli bir yardımcı modül ekleyebiliriz.");return true;
   }
   return false;
 }
}
