package com.jarvis.kisisel;

import android.accessibilityservice.AccessibilityService;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Permission-granted, user-command-only Android screen access.
 * No background capture, no screenshot recording, no network transmission.
 * Can read only accessibility-exposed UI text, not all pixels or private content.
 */
public final class PandoraAccessibilityService extends AccessibilityService {
    private static volatile PandoraAccessibilityService instance;

    public static boolean isConnected() { return instance != null; }

    /** Only a user-initiated notification action may dismiss the open notification panel. */
    public static boolean dismissNotificationShadeIfVisible() {
        PandoraAccessibilityService svc=instance;
        if(svc==null || Build.VERSION.SDK_INT<31) return false;
        AccessibilityNodeInfo root=svc.getRootInActiveWindow();
        if(root==null) return false;
        if(!"com.android.systemui".contentEquals(String.valueOf(root.getPackageName())))
            return false;
        return svc.performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE);
    }

    /** Captures OCR only when explicitly asked; automatically falls back to accessible text. */
    public static void readVisibleTextWithOcr(Consumer<String> callback) {
        PandoraAccessibilityService svc=instance;
        if(svc==null) { callback.accept(""); return; }
        AccessibilityNodeInfo current=svc.getRootInActiveWindow();
        if(current==null ||
                !ScreenSourcePolicy.canRead(String.valueOf(current.getPackageName()),
                    containsPasswordInput(current))) {
            callback.accept(""); return;
        }
        String accessible=readVisibleText();
        PandoraScreenReader.read(svc,accessible,callback);
    }

    public static String readVisibleText() {
        PandoraAccessibilityService svc = instance;
        if (svc == null) return "";
        AccessibilityNodeInfo root = svc.getRootInActiveWindow();
        if (root == null || !ScreenSourcePolicy.canRead(
                String.valueOf(root.getPackageName()), containsPasswordInput(root))) return "";
        try {
            StringBuilder text = new StringBuilder();
            CharSequence pkg = root.getPackageName();
            text.append("Uygulama: ").append(pkg==null?"bilinmiyor":pkg).append("\n");
            HashSet<String> seen = new HashSet<>();
            ArrayDeque<AccessibilityNodeInfo> q = new ArrayDeque<>();
            q.add(root);
            int visited = 0;
            while (!q.isEmpty() && visited++ < 350 && text.length() < 4500) {
                AccessibilityNodeInfo n = q.removeFirst();
                if (n.isPassword() || !n.isVisibleToUser()) continue;
                if (!n.isEditable()) {
                    append(text,seen,n.getText());
                    append(text,seen,n.getContentDescription());
                }
                for(int i=0;i<n.getChildCount();i++) {
                    AccessibilityNodeInfo child = n.getChild(i);
                    if (child != null) q.add(child);
                }
            }
            return text.length() > 4500 ? text.substring(0,4500) : text.toString();
        } finally { /* Android manages accessibility node lifecycle on modern devices. */ }
    }

    private static boolean containsPasswordInput(AccessibilityNodeInfo root) {
        ArrayDeque<AccessibilityNodeInfo> q=new ArrayDeque<>();
        q.add(root);
        int visited=0;
        while(!q.isEmpty() && visited++<350) {
            AccessibilityNodeInfo node=q.removeFirst();
            if(node.isVisibleToUser() && node.isPassword())return true;
            for(int i=0;i<node.getChildCount();i++){
                AccessibilityNodeInfo child=node.getChild(i);
                if(child!=null)q.add(child);
            }
        }
        return false;
    }

    private static void append(StringBuilder text, HashSet<String> seen, CharSequence value) {
        if (value == null) return;
        String item=value.toString().replaceAll("\\s+"," ").trim();
        if(item.length()>1 && item.length()<700 && seen.add(item)) text.append(item).append('\n');
    }

    /** Attempts to use WhatsApp's visible in-call video button, never starts a new call. */
    public static String requestWhatsAppVideoSwitch() {
        PandoraAccessibilityService svc = instance;
        if(svc==null) return "Önce PANDORA ekran erişimi iznini açmalısın.";
        AccessibilityNodeInfo root=svc.getRootInActiveWindow();
        if(root==null) return "Görüntülü görüşmeye geçmek için aktif uygulamanın ekranına erişemedim.";
        String pkg=String.valueOf(root.getPackageName());
        if(!"com.whatsapp".equals(pkg) && !"com.whatsapp.w4b".equals(pkg))
            return "Görüntülü görüşme komutunu yalnızca WhatsApp çağrı ekranında deneyebilirim.";
        if(!callControlsVisible(root)) return "Aktif sesli görüşmeyi doğrulayamadım; işlem yapmadım.";
        AccessibilityNodeInfo video=locateVideoButton(root);
        if(video==null) return "WhatsApp'ın görüntülü görüşmeye geçiş düğmesini bulamadım.";
        AccessibilityNodeInfo clickable=video;
        for(int i=0;i<3 && clickable!=null && !clickable.isClickable();i++)clickable=clickable.getParent();
        if(clickable!=null && clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            return "WhatsApp'taki görüntülü görüşme düğmesine dokundum. Geçişin tamamlanması karşı tarafın onayını gerektirebilir.";
        return "WhatsApp görüntülü görüşmeye geçiş işlemini Android engelledi.";
    }

    private static boolean callControlsVisible(AccessibilityNodeInfo root) {
        ArrayDeque<AccessibilityNodeInfo> nodes=new ArrayDeque<>();nodes.add(root);
        int count=0;boolean end=false,mute=false;
        while(!nodes.isEmpty() && count++<230){
            AccessibilityNodeInfo node=nodes.removeFirst();
            if(!node.isVisibleToUser() || node.isPassword())continue;
            String label=(String.valueOf(node.getText())+" "+String.valueOf(node.getContentDescription()))
                .toLowerCase(new Locale("tr","TR"));
            if(label.contains("aramayı bitir") || label.contains("end call") ||
                label.contains("hang up") || label.contains("arama sonlandır")) end=true;
            if(label.contains("sessize al") || label.contains("mute") ||
                label.contains("hoparlör") || label.contains("speaker")) mute=true;
            for(int i=0;i<node.getChildCount();i++){
                AccessibilityNodeInfo child=node.getChild(i);if(child!=null)nodes.add(child);
            }
        }
        return end && mute;
    }

    private static AccessibilityNodeInfo locateVideoButton(AccessibilityNodeInfo root) {
        ArrayDeque<AccessibilityNodeInfo> nodes=new ArrayDeque<>();nodes.add(root);
        int count=0;
        while(!nodes.isEmpty() && count++<230){
            AccessibilityNodeInfo node=nodes.removeFirst();
            if(!node.isVisibleToUser() || node.isPassword())continue;
            String label=(String.valueOf(node.getText())+" "+String.valueOf(node.getContentDescription()))
                .toLowerCase(new Locale("tr","TR"));
            if(label.contains("görüntülü aramaya geç") ||
                label.contains("görüntülü görüşmeye geç") ||
                label.contains("switch to video") || label.contains("turn on video") ||
                label.contains("görüntülü arama") || label.contains("video call")) return node;
            for(int i=0;i<node.getChildCount();i++){
                AccessibilityNodeInfo child=node.getChild(i);if(child!=null)nodes.add(child);
            }
        }
        return null;
    }

    @Override protected void onServiceConnected() { super.onServiceConnected();instance=this; }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        // Intentionally never capture or store screens automatically.
    }
    @Override public void onInterrupt() {}
    @Override public void onDestroy() {
        if(instance==this)instance=null;
        super.onDestroy();
    }
}
