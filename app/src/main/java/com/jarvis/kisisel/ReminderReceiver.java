package com.jarvis.kisisel;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {
    public static final String CHANNEL = "jarvis_reminders";
    public static void schedule(Context context, int id, String title, long when) {
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if(mgr == null) return;
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("id", id); intent.putExtra("title", title);
        PendingIntent pi = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        mgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, pi);
    }
    public static void cancel(Context context, int id) {
        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if(mgr != null && pi != null) { mgr.cancel(pi); pi.cancel(); }
    }
    public static void ensureChannel(Context ctx) {
        NotificationManager mgr = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if(mgr != null) mgr.createNotificationChannel(new NotificationChannel(CHANNEL, "PANDORA Hatırlatıcıları", NotificationManager.IMPORTANCE_HIGH));
    }
    @Override public void onReceive(Context context, Intent intent) {
        int id = intent.getIntExtra("id", 0);
        String title = intent.getStringExtra("title");
        if (id <= 0 || title == null) return;
        ensureChannel(context);
        Intent start = new Intent(context, MainActivity.class);
        PendingIntent open = PendingIntent.getActivity(context, 0, start, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("PANDORA hatırlatıyor")
                .setContentText(title)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build();
        NotificationManager mgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(mgr != null) {
            try { mgr.notify(id, n); } catch (SecurityException ignored) { }
        }
        new MemoryDb(context).delivered(id);
    }
}
