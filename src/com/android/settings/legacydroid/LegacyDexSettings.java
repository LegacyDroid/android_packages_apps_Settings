package com.android.settings.legacydroid;

import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import android.view.IWindowManager;
import android.view.Surface;

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

    private static final String KEY_MASTER = "dex_enable";

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

        SwitchPreference master = findPreference(KEY_MASTER);
        if (master != null) {
            master.setChecked(SystemProperties.getBoolean(PROP_PC_MODE, false));
            master.setOnPreferenceChangeListener((pref, newValue) -> {
                boolean enabled = (Boolean) newValue;
                setLegacyDexEnabled(enabled);
                return true;
            });
        }
    }

    private void setLegacyDexEnabled(boolean enabled) {
        SystemProperties.set(PROP_PC_MODE, enabled ? "true" : "false");
        SystemProperties.set(PROP_BD_SYSTEMUI, enabled ? "true" : "false");

        IWindowManager wm = IWindowManager.Stub.asInterface(
                ServiceManager.getService("window"));
        try {
            if (enabled) {
                wm.freezeRotation(Surface.ROTATION_90);
            } else {
                wm.thawRotation();
            }
        } catch (RemoteException e) {
            android.provider.Settings.System.putInt(getContext().getContentResolver(),
                    android.provider.Settings.System.USER_ROTATION,
                    enabled ? Surface.ROTATION_90 : Surface.ROTATION_0);
        }

        restartSystemUI();
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
