package com.jarvis.kisisel;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/**
 * Optional Quick Settings entry, explicitly added by the user.
 * Opens a short-lived trampoline so the shade closes before screen capture.
 */
public final class PandoraQuickTileService extends TileService {
    @Override public void onStartListening() {
        super.onStartListening();
        Tile tile=getQsTile();
        if(tile==null)return;
        tile.setState(Tile.STATE_ACTIVE);
        tile.setLabel("PANDORA · Ekrana Bak");
        tile.updateTile();
    }

    @Override public void onClick() {
        super.onClick();
        if(isLocked()) {
            unlockAndRun(this::open);
        } else {
            open();
        }
    }

    private void open() {
        boolean canAnalyze=PandoraTilePolicy.canAnalyzeFromTile(
            WakeMonitorService.running, PandoraAccessibilityService.isConnected());
        Intent intent;
        if(canAnalyze) {
            intent=new Intent(this,PandoraQuickActionActivity.class)
                .setAction(WakeMonitorService.ACTION_SCREEN);
        } else {
            intent=new Intent(this,MainActivity.class)
                .putExtra("pandora_quick_tile_setup",true);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        try {
            if(Build.VERSION.SDK_INT>=34) {
                PendingIntent pending=PendingIntent.getActivity(this,173,intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                startActivityAndCollapse(pending);
            } else {
                startActivityAndCollapse(intent);
            }
        } catch(RuntimeException ignored) {
            // Some OEMs restrict launching from tiles. Don't claim the action ran.
        }
    }
}
