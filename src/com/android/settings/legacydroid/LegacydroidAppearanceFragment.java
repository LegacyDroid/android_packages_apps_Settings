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

    private static final String KEY_LIQUID_GLASS = "legacydroid_liquid_glass";
    private static final String SETTING_LIQUID_GLASS = KEY_LIQUID_GLASS;

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
        initLiquidGlassPreference();
    }

    private void initLiquidGlassPreference() {
        final SwitchPreferenceCompat liquidGlass = findPreference(KEY_LIQUID_GLASS);
        if (liquidGlass == null) {
            return;
        }
        liquidGlass.setChecked(Settings.Global.getInt(
                getContentResolver(), SETTING_LIQUID_GLASS, 1) == 1);
        liquidGlass.setOnPreferenceChangeListener((preference, newValue) -> {
            Settings.Global.putInt(getContentResolver(), SETTING_LIQUID_GLASS,
                    (Boolean) newValue ? 1 : 0);
            return true;
        });
    }
}
