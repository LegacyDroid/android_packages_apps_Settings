package com.android.settings.legacydroid;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemProperties;

import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

public class AppearanceSettings extends DashboardFragment {

    private static final String TAG = "AppearanceSettings";
    private static final String PROP_FLUID_ANIM = "persist.sys.fluid_animations.enabled";
    private static final String KEY_FLUID_ANIM = "fluid_animations";

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
        return R.xml.appearance_settings;
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

        SwitchPreference fluidAnim = findPreference(KEY_FLUID_ANIM);
        if (fluidAnim != null) {
            fluidAnim.setChecked(SystemProperties.getBoolean(PROP_FLUID_ANIM, false));
            fluidAnim.setOnPreferenceChangeListener((pref, newValue) -> {
                SystemProperties.set(PROP_FLUID_ANIM, (Boolean) newValue ? "true" : "false");
                return true;
            });
        }
    }
}
