package com.android.settings.legacydroid;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.List;

public class ProfileCustomizationFragment extends DashboardFragment {

    private static final String TAG = "ProfileCustomization";
    private static final String ARG_PROFILE = "profile";
    private static final String PREFS_NAME = "legacy_droid_customization";

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
        return R.xml.profile_customization;
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

        Bundle args = getArguments();
        String profile = args != null ? args.getString(ARG_PROFILE, "performance") : "performance";

        String titleKey = "legacy_droid_profile_" + profile;
        int titleResId = getResources().getIdentifier(titleKey, "string",
                getContext().getPackageName());
        if (titleResId != 0) {
            getPreferenceScreen().setTitle(getText(titleResId));
        }

        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, 0);

        String[] actionKeys = {
                "action_stop_apps",
                "action_pause_sync",
                "action_high_refresh",
                "action_game_driver",
                "action_dnd",
                "action_low_refresh"
        };

        for (String key : actionKeys) {
            SwitchPreference sp = findPreference(key);
            if (sp != null) {
                boolean saved = prefs.getBoolean(profile + "_" + key, sp.isChecked());
                sp.setChecked(saved);
                sp.setOnPreferenceChangeListener((preference, newValue) -> {
                    prefs.edit().putBoolean(profile + "_" + key, (Boolean) newValue).apply();
                    return true;
                });
            }
        }
    }

    public static Bundle createArgs(String profile) {
        Bundle args = new Bundle();
        args.putString(ARG_PROFILE, profile);
        return args;
    }
}
