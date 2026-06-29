package com.android.settings.legacydroid;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemProperties;

import androidx.preference.ListPreference;
import androidx.preference.SwitchPreference;

import com.android.settings.widget.SeekBarPreference;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

public class AppearanceSettings extends DashboardFragment {

    private static final String TAG = "AppearanceSettings";
    private static final String PROP_BLUR_ENABLED = "persist.sys.legacyblur.enabled";
    private static final String PROP_BLUR_RADIUS = "persist.sys.legacyblur.radius";
    private static final String PROP_BLUR_BACKEND = "persist.sys.legacyblur.backend";

    private static final String KEY_BLUR_ENABLE = "blur_enable";
    private static final String KEY_BLUR_RADIUS = "blur_radius";
    private static final String KEY_BLUR_BACKEND = "blur_backend";

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

        SwitchPreference blurEnable = findPreference(KEY_BLUR_ENABLE);
        if (blurEnable != null) {
            blurEnable.setChecked(SystemProperties.getBoolean(PROP_BLUR_ENABLED, true));
            blurEnable.setOnPreferenceChangeListener((pref, newValue) -> {
                boolean enabled = (Boolean) newValue;
                SystemProperties.set(PROP_BLUR_ENABLED, enabled ? "true" : "false");
                return true;
            });
        }

        SeekBarPreference blurRadius = findPreference(KEY_BLUR_RADIUS);
        if (blurRadius != null) {
            blurRadius.setMax(50);
            blurRadius.setMin(1);
            int val = SystemProperties.getInt(PROP_BLUR_RADIUS, 25);
            blurRadius.setProgress(val);
            blurRadius.setSummary(Integer.toString(val));
            blurRadius.setOnPreferenceChangeListener((pref, newValue) -> {
                int radius = (Integer) newValue;
                SystemProperties.set(PROP_BLUR_RADIUS, Integer.toString(radius));
                pref.setSummary(Integer.toString(radius));
                return true;
            });
        }

        ListPreference blurBackend = findPreference(KEY_BLUR_BACKEND);
        if (blurBackend != null) {
            String val = SystemProperties.get(PROP_BLUR_BACKEND, "0");
            blurBackend.setValue(val);
            blurBackend.setOnPreferenceChangeListener((pref, newValue) -> {
                SystemProperties.set(PROP_BLUR_BACKEND, (String) newValue);
                return true;
            });
        }
    }
}
