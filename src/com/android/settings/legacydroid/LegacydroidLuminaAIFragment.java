/*
 * SPDX-FileCopyrightText: 2026 The LegacyDroid Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.legacydroid;

import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;

/**
 * LuminaAI options: power-button trigger gate and overlay preview.
 *
 * The switch only records the user's intent for now; the consumer that
 * actually replaces the power-button long-press action lands in a later
 * framework milestone (PhoneWindowManager).
 */
public class LegacydroidLuminaAIFragment extends DashboardFragment {

    private static final String TAG = "LegacydroidLuminaAI";

    private static final String KEY_POWER_BUTTON = "legacydroid_luminaai_power_button";
    private static final String SETTING_POWER_BUTTON = "legacydroid_luminaai_power_button";
    private static final String KEY_PREVIEW = "legacydroid_luminaai_preview";

    private static final String ACTION_LUMINA_TRIGGER = "com.legacydroid.luminaai.TRIGGER";
    private static final String LUMINA_PACKAGE = "com.legacydroid.luminaai";
    private static final String LUMINA_RECEIVER = "com.legacydroid.luminaai.LuminaTriggerReceiver";

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.legacydroid_luminaai;
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
        initPowerButtonPreference();
        initPreviewPreference();
    }

    private void initPowerButtonPreference() {
        final SwitchPreferenceCompat powerButton = findPreference(KEY_POWER_BUTTON);
        if (powerButton == null) {
            return;
        }
        powerButton.setChecked(Settings.Global.getInt(
                getContentResolver(), SETTING_POWER_BUTTON, 0) == 1);
        powerButton.setOnPreferenceChangeListener((preference, newValue) -> {
            Settings.Global.putInt(getContentResolver(), SETTING_POWER_BUTTON,
                    (Boolean) newValue ? 1 : 0);
            return true;
        });
    }

    private void initPreviewPreference() {
        final Preference preview = findPreference(KEY_PREVIEW);
        if (preview == null) {
            return;
        }
        preview.setOnPreferenceClickListener(preference -> {
            final Context context = getContext();
            if (context == null) {
                return true;
            }
            final Intent intent = new Intent(ACTION_LUMINA_TRIGGER);
            intent.setComponent(new ComponentName(LUMINA_PACKAGE, LUMINA_RECEIVER));
            context.sendBroadcast(intent);
            return true;
        });
    }
}