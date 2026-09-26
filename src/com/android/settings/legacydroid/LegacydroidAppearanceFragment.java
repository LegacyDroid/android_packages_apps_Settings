/*
 * SPDX-FileCopyrightText: 2026 The LegacyDroid Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.legacydroid;

import android.app.settings.SettingsEnums;
import android.os.Bundle;
import android.provider.Settings;

import androidx.preference.SwitchPreferenceCompat;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;

/** LegacyDroid appearance settings. */
public class LegacydroidAppearanceFragment extends DashboardFragment {

    private static final String TAG = "LegacydroidAppearance";

    private static final String KEY_BLUR = "legacydroid_blur";
    private static final String SETTING_BLUR = KEY_BLUR;

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.legacydroid_appearance;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_HOMEPAGE;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        initBlurPreference();
    }

    private void initBlurPreference() {
        final SwitchPreferenceCompat blur = findPreference(KEY_BLUR);
        if (blur == null) {
            return;
        }
        blur.setChecked(Settings.Global.getInt(
                getContentResolver(), SETTING_BLUR, 1) == 1);
        blur.setOnPreferenceChangeListener((preference, newValue) -> {
            Settings.Global.putInt(getContentResolver(), SETTING_BLUR,
                    (Boolean) newValue ? 1 : 0);
            return true;
        });
    }
}
