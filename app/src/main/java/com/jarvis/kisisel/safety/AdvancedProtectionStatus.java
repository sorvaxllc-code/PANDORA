package com.jarvis.kisisel.safety;

import android.content.Context;
import android.os.Build;
import java.lang.reflect.Method;

/** Android 16+ Advanced Protection status. Never report disabled when
 * an API or permission cannot be queried. Older Android = unsupported. */
public final class AdvancedProtectionStatus {
    public enum State { ENABLED, DISABLED, UNSUPPORTED, UNKNOWN }
    private AdvancedProtectionStatus(){}
    public static State query(Context context){
        if(Build.VERSION.SDK_INT<36)return State.UNSUPPORTED;
        try{
            Class<?> api=Class.forName(
                "android.security.advancedprotection.AdvancedProtectionManager");
            Object manager=context.getSystemService(api);
            if(manager==null)return State.UNKNOWN;
            Method method=api.getMethod("isAdvancedProtectionEnabled");
            Object enabled=method.invoke(manager);
            if(!(enabled instanceof Boolean))return State.UNKNOWN;
            return ((Boolean)enabled)?State.ENABLED:State.DISABLED;
        }catch(Exception | LinkageError e){
            return State.UNKNOWN;
        }
    }
    public static String label(State state){
        switch(state){
            case ENABLED:return "AÇIK · Bazı işlemler Android tarafından kısıtlanabilir";
            case DISABLED:return "KAPALI";
            case UNSUPPORTED:return "Bu Android sürümünde tespit edilemiyor (Android 16+ gerekli)";
            default:return "BİLİNMİYOR · Yetki veya sistem API'si erişilemiyor";
        }
    }
}
