package com.jarvis.kisisel;
import android.app.Activity;
import android.content.*;
/** User-command entry point shared by foreground and voice sessions. */
public final class DeviceTaskRouter {
    private DeviceTaskRouter(){}
    public static String tryHandle(Context c,String command){
        DeviceTaskIntent.Kind kind=DeviceTaskIntent.classify(command);
        if(kind==DeviceTaskIntent.Kind.NONE)return null;
        return open(c,command,kind.name());
    }
    public static String videoOptions(Context c){return open(c,"Görüntülü görüşmeye geç","VIDEO");}
    private static String open(Context c,String command,String kind){
        if(!(c instanceof Activity) && !OverlayPulse.permitted(c))
            return "Bu işlem için PANDORA'yı açıp komutunu yazabilir veya ekran üstünde gösterme iznini açabilirsin.";
        try{
            Intent i=new Intent(c,DeviceTaskActivity.class).putExtra("command",command).putExtra("kind",kind);
            if(!(c instanceof Activity))i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
            return "İşlem seçeneklerini açmayı istedim. Henüz mesaj gönderilmedi veya arama değiştirilmedi.";
        }catch(RuntimeException e){return "Android işlem penceresini açmayı engelledi. Komutu PANDORA içinden verebilirsin.";}
    }
}
