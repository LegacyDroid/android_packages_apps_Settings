package com.android.settings.legacydroid;

import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.Log;

import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

public class LegacyDexSettings extends DashboardFragment {

    private static final String TAG = "LegacyDexSettings";
    private static final String PROP_PC_MODE = "persist.sys.pcmode.enabled";
    private static final String PROP_BD_SYSTEMUI = "persist.sys.systemuiplugin.enabled";

    private static final String KEY_PC_MODE = "dex_enable_pc_mode";
    private static final String KEY_PLUGIN = "dex_enable_plugin";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_HOMEPAGE;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.legacy_dex_settings;
    }

    @Override
    public int getHelpResource() {
        return 0;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return new ArrayList<>();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);

        SwitchPreference pcMode = findPreference(KEY_PC_MODE);
        if (pcMode != null) {
            pcMode.setChecked(SystemProperties.getBoolean(PROP_PC_MODE, false));
            pcMode.setOnPreferenceChangeListener((pref, newValue) -> {
                boolean enabled = (Boolean) newValue;
                SystemProperties.set(PROP_PC_MODE, enabled ? "true" : "false");
                return true;
            });
        }

        SwitchPreference plugin = findPreference(KEY_PLUGIN);
        if (plugin != null) {
            plugin.setChecked(SystemProperties.getBoolean(PROP_BD_SYSTEMUI, false));
            plugin.setOnPreferenceChangeListener((pref, newValue) -> {
                boolean enabled = (Boolean) newValue;
                SystemProperties.set(PROP_BD_SYSTEMUI, enabled ? "true" : "false");
                restartSystemUI();
                return true;
            });
        }
    }

    private void restartSystemUI() {
        Context context = getContext();
        if (context == null) return;
        Intent intent = new Intent("com.android.systemui.action.RESTART");
        intent.setData(Uri.parse("package://" + context.getPackageName()));
        intent.setComponent(new ComponentName("com.android.systemui",
                "com.android.systemui.SysuiRestartReceiver"));
        context.sendBroadcast(intent);
    }
}
