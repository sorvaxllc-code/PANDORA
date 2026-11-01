package com.jarvis.kisisel;

import android.accessibilityservice.AccessibilityService;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import java.util.List;
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

    /**
     * Android may briefly report the notification shade or our finished Quick Tile
     * trampoline as the active root. Retrieve the focused application window
     * instead, but never read an app behind an active PANDORA window.
     */
    private AccessibilityNodeInfo currentAppRoot() {
        AccessibilityNodeInfo direct=getRootInActiveWindow();
        String directPackage=direct==null?null:String.valueOf(direct.getPackageName());
        if(ScreenSourcePolicy.canRead(directPackage,false))return direct;
        List<AccessibilityWindowInfo> windows=getWindows();
        if(windows!=null) {
            // Only a focused or active application window counts; a background
            // app visible behind Quick Settings or PANDORA cannot be captured.
            for(AccessibilityWindowInfo window:windows) {
                if(window.getType()!=AccessibilityWindowInfo.TYPE_APPLICATION ||
                        (!window.isActive() && !window.isFocused()))continue;
                AccessibilityNodeInfo root=window.getRoot();
                if(root==null)continue;
                String pkg=String.valueOf(root.getPackageName());
                if(pkg.startsWith("com.jarvis.kisisel"))return direct;
                if(ScreenSourcePolicy.canRead(pkg,false))return root;
            }
        }
        return direct;
    }

    /** Foreground package only; this method does not read screen contents. */
    public static String foregroundPackage() {
        PandoraAccessibilityService svc=instance;
        if(svc==null)return null;
        AccessibilityNodeInfo root=svc.currentAppRoot();
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

    /** On-demand OCR restricted to the app that was foreground when the request started. */
    public static void readVisibleTextWithOcr(String expectedPackage, Consumer<String> callback) {
        PandoraAccessibilityService svc=instance;
        if(svc==null) { callback.accept(""); return; }
        AccessibilityNodeInfo current=svc.currentAppRoot();
        if(current==null || !ScreenCapturePolicy.canUse(expectedPackage,
                String.valueOf(current.getPackageName()),containsPasswordInput(current))) {
            callback.accept(""); return;
        }
        String accessible=readVisibleText();
        // Screenshot + OCR can take seconds. An app change or a newly visible
        // password form must invalidate the result before it reaches the AI.
        PandoraScreenReader.read(svc,accessible,extracted -> {
            PandoraAccessibilityService active=instance;
            AccessibilityNodeInfo latest=active==null?null:active.currentAppRoot();
            if(active!=svc || latest==null || !ScreenCapturePolicy.canUse(expectedPackage,
                    String.valueOf(latest.getPackageName()),containsPasswordInput(latest))) {
                callback.accept("");
            } else {
                callback.accept(extracted);
            }
        });
    }

    /** Captures pixels only after an explicit request; never writes a screenshot to disk. */
    public static void captureImage(String expectedPackage,Consumer<String> callback){
        PandoraAccessibilityService svc=instance;
        if(svc==null || Build.VERSION.SDK_INT<30){callback.accept(null);return;}
        AccessibilityNodeInfo root=svc.currentAppRoot();
        if(root==null || !ScreenCapturePolicy.canUse(expectedPackage,String.valueOf(root.getPackageName()),containsPasswordInput(root))){callback.accept(null);return;}
        try{svc.takeScreenshot(android.view.Display.DEFAULT_DISPLAY,svc.getMainExecutor(),new TakeScreenshotCallback(){
            public void onFailure(int code){callback.accept(null);}
            public void onSuccess(ScreenshotResult result){
                String encoded=null;
                android.graphics.Bitmap hardware=null,copy=null,small=null;
                try{
                    AccessibilityNodeInfo latest=svc.currentAppRoot();
                    android.app.KeyguardManager lock=svc.getSystemService(android.app.KeyguardManager.class);
                    if(instance!=svc || latest==null || lock!=null&&lock.isDeviceLocked() ||
                        !ScreenCapturePolicy.canUse(expectedPackage,String.valueOf(latest.getPackageName()),containsPasswordInput(latest)))return;
                    hardware=android.graphics.Bitmap.wrapHardwareBuffer(result.getHardwareBuffer(),result.getColorSpace());
                    if(hardware==null)return;
                    copy=hardware.copy(android.graphics.Bitmap.Config.ARGB_8888,false);
                    float scale=Math.min(1f,1280f/Math.max(copy.getWidth(),copy.getHeight()));
                    small=android.graphics.Bitmap.createScaledBitmap(copy,Math.max(1,(int)(copy.getWidth()*scale)),Math.max(1,(int)(copy.getHeight()*scale)),true);
                    java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
                    small.compress(android.graphics.Bitmap.CompressFormat.JPEG,80,bytes);
                    encoded=android.util.Base64.encodeToString(bytes.toByteArray(),android.util.Base64.NO_WRAP);
                }finally{
                    if(small!=null && small!=copy)small.recycle();if(copy!=null)copy.recycle();if(hardware!=null)hardware.recycle();
                    result.getHardwareBuffer().close();callback.accept(encoded);
                }
            }
        });}catch(RuntimeException error){callback.accept(null);}
    }

    public static String readVisibleText() {
        PandoraAccessibilityService svc = instance;
        if (svc == null) return "";
        AccessibilityNodeInfo root = svc.currentAppRoot();
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
        if(!WhatsAppCallPolicy.supportedPackage(foregroundPackage()))return "WhatsApp görüşme ekranı açık değil; işlem yapılmadı.";
        return requestVideoSwitch();
    }
    public static String requestVideoSwitch() {
        PandoraAccessibilityService svc = instance;
        if(svc==null) return "Önce PANDORA ekran erişimi iznini açmalısın.";
        AccessibilityNodeInfo root=svc.currentAppRoot();
        if(root==null) return "Görüntülü görüşmeye geçmek için aktif uygulamanın ekranına erişemedim.";
        String pkg=String.valueOf(root.getPackageName());
        CallControls controls = inspectCallControls(root);
        if (controls.activeVideo)
            return "Görüntülü görüşme zaten etkin görünüyor; işlem yapmadım.";
        if(!(controls.endCall && controls.audioControl && !controls.activeVideo))
            return "Aktif sesli görüşmeyi güvenilir biçimde doğrulayamadım; işlem yapmadım.";
        AccessibilityNodeInfo video=locateVideoButton(root);
        if(video==null) return "Görüşmenin görüntülüye geçiş düğmesini bulamadım.";
        AccessibilityNodeInfo clickable=video;
        // One parent at most: never click a large, unrelated container.
        if(!clickable.isClickable()) clickable=clickable.getParent();
        if(clickable!=null && clickable.isVisibleToUser() && clickable.isEnabled()
                && clickable.isClickable()
                && clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            return "Görüntülü görüşmeye geçiş düğmesine dokunma isteğini Android'e ilettim. İşlemin tamamlandığını doğrulamadım; karşı tarafın onayı gerekebilir.";
        return "Görüntülü görüşmeye geçiş işlemini Android engelledi.";
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
