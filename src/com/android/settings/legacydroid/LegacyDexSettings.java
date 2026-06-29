package com.android.settings.legacydroid;

import android.app.AlertDialog;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
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
    private static final String KEY_FORCE_RESIZE = "dex_force_resize";

    private SwitchPreference mMasterPref;

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

        mMasterPref = findPreference(KEY_MASTER);
        if (mMasterPref != null) {
            mMasterPref.setChecked(SystemProperties.getBoolean(PROP_PC_MODE, false));
            mMasterPref.setOnPreferenceChangeListener((pref, newValue) -> {
                boolean enabled = (Boolean) newValue;
                if (enabled) {
                    showWarningDialog();
                    return false;
                } else {
                    setLegacyDexEnabled(false);
                    return true;
                }
            });
        }

        SwitchPreference forceResize = findPreference(KEY_FORCE_RESIZE);
        if (forceResize != null) {
            int val = Settings.Global.getInt(getContext().getContentResolver(),
                    Settings.Global.DEVELOPMENT_FORCE_RESIZABLE_ACTIVITIES, 0);
            forceResize.setChecked(val != 0);
            forceResize.setOnPreferenceChangeListener((pref, newValue) -> {
                boolean enabled = (Boolean) newValue;
                Settings.Global.putInt(getContext().getContentResolver(),
                        Settings.Global.DEVELOPMENT_FORCE_RESIZABLE_ACTIVITIES,
                        enabled ? 1 : 0);
                return true;
            });
        }
    }

    private void showWarningDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.legacy_dex_warning_title)
                .setMessage(R.string.legacy_dex_warning_message)
                .setPositiveButton(R.string.legacy_dex_warning_accept,
                        (dialog, which) -> {
                            mMasterPref.setChecked(true);
                            setLegacyDexEnabled(true);
                        })
                .setNegativeButton(android.R.string.cancel,
                        (dialog, which) -> mMasterPref.setChecked(false))
                .setOnDismissListener(dialog -> {
                    if (!SystemProperties.getBoolean(PROP_PC_MODE, false)) {
                        mMasterPref.setChecked(false);
                    }
                })
                .show();
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
