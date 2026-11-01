package com.jarvis.kisisel;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

/**
 * Close Quick Settings before reading underlying app; never holds or records content.
 * Launched only by PANDORA's explicit user-clicked Quick Settings tile.
 */
public final class PandoraQuickActionActivity extends Activity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        final String action=getIntent()==null?null:getIntent().getAction();
        finish();
        if(!WakeMonitorService.ACTION_SCREEN.equals(action))return;
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            PandoraScreenAnalysisController.request(getApplicationContext(),
                answer -> PandoraHeadphoneReply.say(getApplicationContext(),answer));
        },650);
    }
}
