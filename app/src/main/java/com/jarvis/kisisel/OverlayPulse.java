package com.jarvis.kisisel;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.*;
import android.provider.Settings;
import android.view.*;
/** Brief, non-interactive monochrome acknowledgement. Never opens an Activity. */
public final class OverlayPulse {
    private static boolean visible;
    private OverlayPulse(){}
    public static boolean permitted(Context c){return Settings.canDrawOverlays(c);}
    public static void show(Context c){
        Context app=c.getApplicationContext();
        new Handler(Looper.getMainLooper()).post(()->{
            if(visible || !permitted(app))return;
            WindowManager wm=(WindowManager)app.getSystemService(Context.WINDOW_SERVICE);
            if(wm==null)return;
            OrbView wave=new OrbView(app);wave.setActive(true);
            int size=(int)(220*app.getResources().getDisplayMetrics().density);
            WindowManager.LayoutParams p=new WindowManager.LayoutParams(size,size,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,PixelFormat.TRANSLUCENT);
            p.gravity=Gravity.CENTER;p.alpha=.75f;
            try{
                wm.addView(wave,p);visible=true;
                new Handler(Looper.getMainLooper()).postDelayed(()->{
                    wave.setActive(false);
                    try{wm.removeView(wave);}catch(RuntimeException ignored){}finally{visible=false;}
                },1800);
            }catch(RuntimeException ignored){visible=false;}
        });
    }
}
