package com.jarvis.kisisel;

import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import android.hardware.HardwareBuffer;
import android.os.Build;
import android.view.Display;
import android.accessibilityservice.AccessibilityService;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google.mlkit.vision.text.TextRecognizer;
import java.util.function.Consumer;

/**
 * On-demand, on-device OCR. Only called after the user's command and accessibility permission.
 * Screenshots never leave RAM; only extracted text can reach the selected AI model
 * after its separate cloud-consent policy allows it.
 */
public final class PandoraScreenReader {
    private PandoraScreenReader() {}

    public static void read(PandoraAccessibilityService service,
                            String accessibleText, Consumer<String> callback) {
        if(service==null || Build.VERSION.SDK_INT<30) {
            callback.accept(accessibleText);
            return;
        }
        // Screen capture is denied for some protected/secure windows by Android.
        try {
            service.takeScreenshot(Display.DEFAULT_DISPLAY, service.getMainExecutor(),
                new AccessibilityService.TakeScreenshotCallback() {
                    @Override public void onFailure(int errorCode) {
                        callback.accept(accessibleText);
                    }
                    @Override public void onSuccess(AccessibilityService.ScreenshotResult image) {
                        HardwareBuffer hardware=image.getHardwareBuffer();
                        Bitmap bitmap=null;
                        try {
                            ColorSpace color=image.getColorSpace();
                            if(color==null)color=ColorSpace.get(ColorSpace.Named.SRGB);
                            Bitmap wrapped=Bitmap.wrapHardwareBuffer(hardware,color);
                            if(wrapped!=null) bitmap=wrapped.copy(Bitmap.Config.ARGB_8888,false);
                        } catch (RuntimeException | OutOfMemoryError ignored) {
                            bitmap=null;
                        } finally {
                            if(hardware!=null)hardware.close();
                        }
                        if(bitmap==null) {
                            callback.accept(accessibleText);
                            return;
                        }
                        final Bitmap screenshot=bitmap;
                        final TextRecognizer recognizer=TextRecognition.getClient(
                            TextRecognizerOptions.DEFAULT_OPTIONS);
                        try {
                            recognizer.process(InputImage.fromBitmap(screenshot,0))
                                .addOnSuccessListener(result -> {
                                    String text=result.getText();
                                    recognizer.close();
                                    screenshot.recycle();
                                    callback.accept(merge(accessibleText,text));
                                }).addOnFailureListener(ex -> {
                                    recognizer.close();
                                    screenshot.recycle();
                                    callback.accept(accessibleText);
                                });
                        } catch(RuntimeException ex) {
                            recognizer.close();
                            screenshot.recycle();
                            callback.accept(accessibleText);
                        }
                    }
                });
        } catch (RuntimeException ex) {
            callback.accept(accessibleText);
        }
    }

    static String merge(String accessible,String ocr) {
        String a=accessible==null?"":accessible.trim();
        String o=ocr==null?"":ocr.trim();
        // The OCR is intentionally truncated; screenshots themselves are never persisted.
        if(o.length()<45) return limit(a);
        if(a.length()<80) return limit("Ekran OCR metni:\n"+o);
        if(a.contains(o) || o.contains(a)) return limit(o.length()>a.length()?o:a);
        return limit(a+"\nEkran OCR metni:\n"+o);
    }
    private static String limit(String text) {
        return text.length()>5000 ? text.substring(0,5000) : text;
    }
}
