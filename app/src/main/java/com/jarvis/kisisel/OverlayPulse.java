package com.jarvis.kisisel;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.graphics.PixelFormat;

/**
 * User-approved, touch-through visual feedback on a recognized wake phrase.
 * It does not read other apps' screens and never intercepts taps.
 */
public final class OverlayPulse {
    private OverlayPulse() {}
    public static boolean permitted(Context context) {
        return Settings.canDrawOverlays(context);
    }

    public static void show(Context context) {
        if (!permitted(context)) return;
        Context app = context.getApplicationContext();
        new Handler(Looper.getMainLooper()).post(() -> {
            if (!permitted(app)) return;
            WindowManager manager = (WindowManager) app.getSystemService(Context.WINDOW_SERVICE);
            if (manager == null) return;
            final View border = new BlueBorder(app);
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                  | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                  | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                  | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.TOP | Gravity.START;
            // Android 12+ may block touch-through overlays that exceed opacity 0.8.
            params.alpha = 0.65f;
            try {
                manager.addView(border, params);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    try { manager.removeView(border); }
                    catch (IllegalArgumentException ignored) {}
                }, 2500);
            } catch (RuntimeException ignored) {
                // Android/OEM can deny an overlay even after the user granted permission.
            }
        });
    }

    private static final class BlueBorder extends View {
        private final Paint brush = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final long createdAt = SystemClock.uptimeMillis();
        private final RectF bounds = new RectF();
        BlueBorder(Context context) { super(context); setWillNotDraw(false); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float density = getResources().getDisplayMetrics().density;
            float elapsed = SystemClock.uptimeMillis() - createdAt;
            float fade = Math.max(0f, Math.min(1f, (2500f-elapsed) / 650f));
            float wave = 0.5f + 0.5f * (float)Math.sin(elapsed / 220f);
            float inset = 8f*density;
            float corner = 28f*density;
            bounds.set(inset,inset,getWidth()-inset,getHeight()-inset);
            brush.setStyle(Paint.Style.STROKE);
            brush.setStrokeWidth((8f+5f*wave)*density);
            brush.setColor(android.graphics.Color.argb((int)(105*fade),40,128,255));
            canvas.drawRoundRect(bounds,corner,corner,brush);
            brush.setStrokeWidth((2.5f+1.4f*wave)*density);
            brush.setColor(android.graphics.Color.argb((int)(245*fade),115,195,255));
            canvas.drawRoundRect(bounds,corner,corner,brush);
            if (elapsed < 2500) postInvalidateDelayed(33);
        }
    }
}
