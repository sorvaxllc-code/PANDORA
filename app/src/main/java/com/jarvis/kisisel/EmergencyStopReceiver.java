package com.jarvis.kisisel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.jarvis.kisisel.safety.EmergencyStop;

/** Explicit, non-exported broadcast from PANDORA's own STOP notification. */
public final class EmergencyStopReceiver extends BroadcastReceiver {
    public static final String ACTION_STOP_ALL="com.jarvis.kisisel.EMERGENCY_STOP_ALL";
    @Override public void onReceive(Context context,Intent intent){
        if(intent==null || !ACTION_STOP_ALL.equals(intent.getAction()))return;
        EmergencyStop.stop();
        context.getSharedPreferences("jarvis_settings",Context.MODE_PRIVATE)
            .edit().putBoolean("pandora_auto_wake",false).apply();
        context.stopService(new Intent(context,WakeMonitorService.class));
    }
}
