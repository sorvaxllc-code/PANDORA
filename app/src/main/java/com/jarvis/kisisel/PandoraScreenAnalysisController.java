package com.jarvis.kisisel;
import android.app.KeyguardManager;
import android.content.*;
import android.os.*;
import com.jarvis.kisisel.ai.*;
import java.util.function.Consumer;
/** Explicit, single-shot image reasoning. Only replies are persisted, never pixels. */
public final class PandoraScreenAnalysisController {
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static boolean busy;private static int serial;
    private static HttpAiProvider.CancelToken pending;
    private static MemoryDb database;
    public static void request(Context c,Consumer<String> done){request(c,"Ekrana bak, burada ne anlatılıyor?",done);}
    public static void request(Context c,String question,Consumer<String> done){MAIN.post(()->start(c.getApplicationContext(),question,done));}
    private static void start(Context c,String question,Consumer<String> done){
        if(busy){if(done!=null)done.accept("Önceki ekran analizi sürüyor.");return;}
        SharedPreferences p=c.getSharedPreferences("jarvis_settings",Context.MODE_PRIVATE);
        if(!PandoraAccessibilityService.isConnected()){deliver(c,"Ekran erişimi kapalı. Menüden ekran erişimini açmalısın.",done);return;}
        if(!p.getBoolean("pandora_screen_image_optin",false)){deliver(c,"Görsel analiz için menüdeki Görsel analiz iznini açmalısın.",done);return;}
        if(!("HERMES".equals(p.getString("pandora_mode","OFF"))||"CLOUD".equals(p.getString("pandora_mode","OFF"))||"AUTO".equals(p.getString("pandora_mode","OFF")))){deliver(c,"Görsel analiz şu anda Hermes, bulut veya otomatik mod gerektiriyor.",done);return;}
        busy=true;int id=++serial;
        MAIN.postDelayed(()->finish(c,id,"Ekran analizi zaman aşımına uğradı.",done),145000);
        // Wait for the wake animation/notification shade to disappear before capturing.
        MAIN.postDelayed(()->capture(c,id,question,done),2000);
    }
    private static boolean allowed(Context c,String pkg){
        KeyguardManager lock=c.getSystemService(KeyguardManager.class);
        SharedPreferences p=c.getSharedPreferences("jarvis_settings",Context.MODE_PRIVATE);
        String mode=p.getString("pandora_mode","OFF");
        return !(lock!=null&&lock.isDeviceLocked()) && ("HERMES".equals(mode)||"CLOUD".equals(mode)||"AUTO".equals(mode))
            && p.getBoolean("pandora_screen_image_optin",false) && PandoraAccessibilityService.isConnected()
            && ScreenCapturePolicy.canUse(pkg,PandoraAccessibilityService.foregroundPackage(),false);
    }
    private static void capture(Context c,int id,String question,Consumer<String> done){
        if(!busy||id!=serial)return;
        String pkg=PandoraAccessibilityService.foregroundPackage();
        if(!allowed(c,pkg)){finish(c,id,"Bu ekrana erişilemiyor. Telefonun kilidini açıp incelemek istediğin uygulamaya dönmelisin.",done);return;}
        PandoraAccessibilityService.captureImage(pkg,image->{
            if(!busy||id!=serial)return;
            if(image==null || !allowed(c,pkg)){finish(c,id,"Görüntü alınamadı veya ekran değişti. Korumalı ekranlar analiz edilemez.",done);return;}
            database=new MemoryDb(c);
            pending=new ModelRouter(c,database).askImageAsync(question,image,(answer,source)->MAIN.post(()->finish(c,id,answer,done)));
        });
    }
    public static void cancel(){
        MAIN.post(()->{busy=false;++serial;if(pending!=null)pending.cancel();pending=null;
            if(database!=null)database.close();database=null;});
    }
    private static void finish(Context c,int id,String answer,Consumer<String> done){
        if(!busy||id!=serial)return;busy=false;++serial;
        if(pending!=null)pending.cancel();pending=null;
        if(database!=null)database.close();database=null;
        deliver(c,answer,done);
    }
    private static void deliver(Context c,String answer,Consumer<String> done){
        try(MemoryDb db=new MemoryDb(c)){db.message("pandora",answer);}
        if(done!=null)done.accept(answer);
    }
}
