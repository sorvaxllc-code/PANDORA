package com.jarvis.kisisel;
import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;

public final class PandoraAlerts {
    public static final String ALERT_CHANNEL = "pandora_important_silent_v20";
    public static final String MONITOR_CHANNEL = "pandora_wake_monitor_v1";
    private PandoraAlerts() {}

    public static void ensureChannels(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        NotificationChannel urgent = new NotificationChannel(ALERT_CHANNEL,
                "PANDORA önemli uyarılar", NotificationManager.IMPORTANCE_HIGH);
        urgent.setDescription("Kulaklıksız sessiz, görsel önemli uyarılar.");
        urgent.setSound(null,null);
        urgent.enableVibration(false);
        manager.createNotificationChannel(urgent);
        NotificationChannel monitor = new NotificationChannel(MONITOR_CHANNEL,
                "PANDORA sesli uyandırma", NotificationManager.IMPORTANCE_LOW);
        monitor.setDescription("Deneysel mikrofon dinleme hizmeti çalışırken görünür.");
        manager.createNotificationChannel(monitor);
    }
    public static boolean canNotify(Context context) {
        return Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }
    public static PendingIntent openApp(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(context, 143, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    public static boolean postImportant(Context context, String title, String body) {
        if (!canNotify(context)) return false;
        ensureChannels(context);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return false;
        Notification message = new Notification.Builder(context, ALERT_CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title).setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setAutoCancel(true).setContentIntent(openApp(context))
                .setCategory(Notification.CATEGORY_REMINDER)
                .setVisibility(Notification.VISIBILITY_PRIVATE).build();
        manager.notify(2045, message);
        return true;
    }
    public static Notification wakeNotification(Context context) {
        ensureChannels(context);
        Intent stop = new Intent(context, WakeMonitorService.class);
        stop.setAction(WakeMonitorService.ACTION_STOP);
        PendingIntent stopIntent = PendingIntent.getService(context, 144, stop,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent screen = new Intent(context, WakeMonitorService.class).setAction(WakeMonitorService.ACTION_SCREEN);
        PendingIntent screenIntent = PendingIntent.getService(context, 145, screen,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent video = new Intent(context, WakeMonitorService.class).setAction(WakeMonitorService.ACTION_VIDEO);
        PendingIntent videoIntent = PendingIntent.getService(context, 146, video,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(context, MONITOR_CHANNEL)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("PANDORA · Deneysel sesli uyandırma")
                .setContentText("Mikrofon kullanılıyor · Durdurmak için dokun")
                .setContentIntent(openApp(context))
                .addAction(android.R.drawable.ic_menu_view, "Ekrana bak", screenIntent)
                .addAction(android.R.drawable.ic_menu_camera, "Görüntülüye geç", videoIntent)
                .addAction(android.R.drawable.ic_media_pause, "Durdur", stopIntent)
                .setOngoing(true).setCategory(Notification.CATEGORY_SERVICE)
                .setVisibility(Notification.VISIBILITY_PRIVATE).build();
    }
    public static boolean headsetConnected(Context context) {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audio == null) return false;
        for (AudioDeviceInfo device : audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            switch (device.getType()) {
                case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP:
                case AudioDeviceInfo.TYPE_BLUETOOTH_SCO:
                case AudioDeviceInfo.TYPE_BLE_HEADSET:
                case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
                case AudioDeviceInfo.TYPE_WIRED_HEADSET:
                case AudioDeviceInfo.TYPE_USB_HEADSET:
                    return true;
            }
        }
        return false;
    }
    public static boolean inCall(Context context) {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return audio != null && audio.getMode() != AudioManager.MODE_NORMAL;
    }
}