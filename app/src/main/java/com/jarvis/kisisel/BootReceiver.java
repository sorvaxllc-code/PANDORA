package com.jarvis.kisisel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        for (MemoryDb.Reminder r : new MemoryDb(context).pendingReminders()) {
            ReminderReceiver.schedule(context, r.id, r.title, r.when);
        }
    }
}
