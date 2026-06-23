package com.android.settings.legacydroid;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;

public class LegacyDroidBootReceiver extends BroadcastReceiver {

    private static final String PREFS_NAME = "legacy_droid_prefs";
    private static final String PREFS_CUSTOM = "legacy_droid_customization";
    private static final String KEY_SAVED_PROFILE = "saved_profile";
    private static final String KEY_UPDATABLE_DRIVER = "updatable_driver_all_apps";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String profile = prefs.getString(KEY_SAVED_PROFILE, "balance");
        if ("balance".equals(profile)) return;

        SharedPreferences custom = context.getSharedPreferences(PREFS_CUSTOM, 0);
        ContentResolver cr = context.getContentResolver();

        switch (profile) {
            case "performance":
                if (custom.getBoolean(profile + "_action_pause_sync", true))
                    ContentResolver.setMasterSyncAutomatically(false);
                if (custom.getBoolean(profile + "_action_high_refresh", true)) {
                    try {
                        Settings.System.putFloat(cr, Settings.System.MIN_REFRESH_RATE, 120f);
                        Settings.System.putFloat(cr, Settings.System.PEAK_REFRESH_RATE, 120f);
                    } catch (Exception ignored) {}
                }
                if (custom.getBoolean(profile + "_action_game_driver", true)) {
                    try {
                        Settings.Global.putString(cr, KEY_UPDATABLE_DRIVER, "1");
                    } catch (Exception ignored) {}
                }
                if (custom.getBoolean(profile + "_action_dnd", true)) {
                    try {
                        android.app.NotificationManager nm = (android.app.NotificationManager)
                                context.getSystemService(Context.NOTIFICATION_SERVICE);
                        if (nm != null) {
                            nm.setInterruptionFilter(
                                    android.app.NotificationManager.INTERRUPTION_FILTER_ALARMS);
                        }
                    } catch (Exception ignored) {}
                }
                setAnimationScales(cr, 0f);
                break;

            case "battery_saver":
                if (custom.getBoolean(profile + "_action_low_refresh", true)) {
                    try {
                        Settings.System.putFloat(cr, Settings.System.MIN_REFRESH_RATE, 60f);
                        Settings.System.putFloat(cr, Settings.System.PEAK_REFRESH_RATE, 60f);
                    } catch (Exception ignored) {}
                }
                setAnimationScales(cr, 0f);
                break;
        }
    }

    private void setAnimationScales(ContentResolver cr, float scale) {
        try {
            Settings.Global.putFloat(cr, "window_animation_scale", scale);
            Settings.Global.putFloat(cr, "transition_animation_scale", scale);
            Settings.Global.putFloat(cr, "animator_duration_scale", scale);
        } catch (Exception ignored) {}
    }
}
