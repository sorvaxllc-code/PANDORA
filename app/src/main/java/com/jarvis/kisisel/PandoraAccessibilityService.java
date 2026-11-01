package com.jarvis.kisisel;

import android.accessibilityservice.AccessibilityService;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.function.Consumer;

/**
 * Permission-granted, user-command-only Android screen access.
 * No background capture, no screenshot recording, no network transmission.
 * Can read only accessibility-exposed UI text, not all pixels or private content.
 */
public final class PandoraAccessibilityService extends AccessibilityService {
    private static volatile PandoraAccessibilityService instance;

    public static boolean isConnected() { return instance != null; }

    /** Foreground app for determining whether Quick Settings has returned to the underlying app. */
    public static String foregroundPackage() {
        PandoraAccessibilityService svc=instance;
        if(svc==null)return null;
        AccessibilityNodeInfo root=svc.getRootInActiveWindow();
        if(root==null)return null;
        CharSequence name=root.getPackageName();
        return name==null?null:name.toString();
    }

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
        if(!WhatsAppCallPolicy.supportedPackage(pkg))
            return "Görüntülü görüşme komutunu yalnızca WhatsApp çağrı ekranında deneyebilirim.";
        CallControls controls = inspectCallControls(root);
        if (controls.activeVideo)
            return "WhatsApp'ta görüntülü görüşme zaten etkin görünüyor; işlem yapmadım.";
        if(!WhatsAppCallPolicy.mayUpgrade(pkg,controls.endCall,controls.audioControl,controls.activeVideo))
            return "Aktif sesli görüşmeyi güvenilir biçimde doğrulayamadım; işlem yapmadım.";
        AccessibilityNodeInfo video=locateVideoButton(root);
        if(video==null) return "WhatsApp'ın görüntülü görüşmeye geçiş düğmesini bulamadım.";
        AccessibilityNodeInfo clickable=video;
        // One parent at most: never click a large, unrelated container.
        if(!clickable.isClickable()) clickable=clickable.getParent();
        if(clickable!=null && clickable.isVisibleToUser() && clickable.isEnabled()
                && clickable.isClickable()
                && clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            return "WhatsApp görüntülü görüşmeye geçiş düğmesine dokunma isteğini Android'e ilettim. İşlemin tamamlandığını doğrulamadım; karşı tarafın onayı gerekebilir.";
        return "WhatsApp görüntülü görüşmeye geçiş işlemini Android engelledi.";
    }

    private static final class CallControls {
        boolean endCall, audioControl, activeVideo;
    }
    private static CallControls inspectCallControls(AccessibilityNodeInfo root) {
        CallControls result=new CallControls();
        ArrayDeque<AccessibilityNodeInfo> nodes=new ArrayDeque<>();
        nodes.add(root);
        int count=0;
        while(!nodes.isEmpty() && count++<300) {
            AccessibilityNodeInfo node=nodes.removeFirst();
            if(!node.isVisibleToUser() || node.isPassword())continue;
            String label=String.valueOf(node.getContentDescription())+" "+String.valueOf(node.getText());
            // Only interactive controls can prove a call is active.
            if(node.isEnabled() && (node.isClickable() || node.getParent()!=null
                    && node.getParent().isClickable())) {
                if(WhatsAppCallPolicy.isEndCallControl(label))result.endCall=true;
                if(WhatsAppCallPolicy.isAudioControl(label))result.audioControl=true;
                if(WhatsAppCallPolicy.indicatesActiveVideo(label))result.activeVideo=true;
            }
            for(int i=0;i<node.getChildCount();i++) {
                AccessibilityNodeInfo child=node.getChild(i);
                if(child!=null)nodes.add(child);
            }
        }
        return result;
    }

    private static AccessibilityNodeInfo locateVideoButton(AccessibilityNodeInfo root) {
        ArrayDeque<AccessibilityNodeInfo> nodes=new ArrayDeque<>();
        nodes.add(root);
        int count=0;
        while(!nodes.isEmpty() && count++<300) {
            AccessibilityNodeInfo node=nodes.removeFirst();
            if(!node.isVisibleToUser() || node.isPassword() || !node.isEnabled())continue;
            AccessibilityNodeInfo parent=node.getParent();
            boolean isControl=node.isClickable() ||
                (parent!=null && parent.isEnabled() && parent.isClickable());
            // Use the accessibility label of the candidate button, not unrelated
            // chat or decorative text containing "video call".
            String description=String.valueOf(node.getContentDescription());
            String label=description.equals("null") ? String.valueOf(node.getText()) : description;
            if(WhatsAppCallPolicy.isUpgradeButton(label,isControl))
                return node;
            for(int i=0;i<node.getChildCount();i++) {
                AccessibilityNodeInfo child=node.getChild(i);
                if(child!=null)nodes.add(child);
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
