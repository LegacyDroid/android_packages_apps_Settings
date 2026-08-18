/*
 * SPDX-FileCopyrightText: 2026 The LegacyDroid Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.legacydroid;

import android.app.settings.SettingsEnums;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;

/**
 * LuminaAI options: power-button trigger, overlay preview, AI engine
 * (provider/model/custom OpenAI-compatible endpoint), behavior toggles and
 * per-tool permission switches.
 */
public class LegacydroidLuminaAIFragment extends DashboardFragment {

    private static final String TAG = "LegacydroidLuminaAI";

    private static final String KEY_POWER_BUTTON = "legacydroid_luminaai_power_button";
    private static final String SETTING_POWER_BUTTON = Settings.Global.LUMINA_POWER_BUTTON;
    private static final String KEY_PREVIEW = "legacydroid_luminaai_preview";

    private static final String KEY_PROVIDER = "legacydroid_luminaai_provider";
    private static final String SETTING_PROVIDER = Settings.Global.LUMINA_ENGINE_PROVIDER;
    private static final String KEY_MODEL = "legacydroid_luminaai_model";
    private static final String SETTING_MODEL = Settings.Global.LUMINA_ENGINE_MODEL;
    private static final String KEY_CUSTOM_URL = "legacydroid_luminaai_custom_base_url";
    private static final String SETTING_CUSTOM_URL = Settings.Global.LUMINA_CUSTOM_BASE_URL;
    private static final String KEY_CUSTOM_KEY = "legacydroid_luminaai_custom_api_key";
    private static final String SETTING_CUSTOM_KEY = Settings.Global.LUMINA_CUSTOM_API_KEY;
    private static final String KEY_CUSTOM_MODEL = "legacydroid_luminaai_custom_model";
    private static final String SETTING_CUSTOM_MODEL = Settings.Global.LUMINA_CUSTOM_MODEL;

    private static final String KEY_PRIVACY_ALERTS = "legacydroid_luminaai_privacy_alerts";
    private static final String SETTING_PRIVACY_ALERTS = Settings.Global.LUMINA_PRIVACY_ALERTS;
    private static final String KEY_AUTO_OTP = "legacydroid_luminaai_auto_otp";
    private static final String SETTING_AUTO_OTP = Settings.Global.LUMINA_AUTO_OTP;
    private static final String KEY_DEV_MODE = "legacydroid_luminaai_unsafe_dev_mode";
    private static final String SETTING_DEV_MODE = Settings.Global.LUMINA_UNSAFE_DEV_MODE;

    private static final String KEY_TOOLS_CATEGORY = "legacydroid_luminaai_tools_category";

    private static final String LUMINA_PACKAGE = "com.legacydroid.luminaai";
    private static final String LUMINA_ACTIVITY = "com.legacydroid.luminaai.LuminaOverlayActivity";

    private static final String PROVIDER_CUSTOM = "custom";

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
        initEnginePreferences();
        initBehaviorPreferences();
        initToolPreferences();
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
            // Explicit activity start: a foreground caller is always allowed to
            // launch it, unlike a broadcast to a cold manifest receiver, which
            // the broadcast queue refuses to start in the background.
            final Intent intent = new Intent();
            intent.setComponent(new ComponentName(LUMINA_PACKAGE, LUMINA_ACTIVITY));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                context.startActivity(intent);
            } catch (ActivityNotFoundException | SecurityException e) {
                Log.w(TAG, "LuminaAI preview unavailable", e);
            }
            return true;
        });
    }

    private void initEnginePreferences() {
        final ListPreference provider = findPreference(KEY_PROVIDER);
        final ListPreference model = findPreference(KEY_MODEL);
        final EditTextPreference customUrl = findPreference(KEY_CUSTOM_URL);
        final EditTextPreference customKey = findPreference(KEY_CUSTOM_KEY);
        final EditTextPreference customModel = findPreference(KEY_CUSTOM_MODEL);
        if (provider == null) {
            return;
        }
        final String providerValue = Settings.Global.getString(getContentResolver(), SETTING_PROVIDER);
        provider.setValue(providerValue != null ? providerValue : "gemini");
        provider.setSummary(provider.getEntry());
        provider.setOnPreferenceChangeListener((preference, newValue) -> {
            final String value = (String) newValue;
            Settings.Global.putString(getContentResolver(), SETTING_PROVIDER, value);
            provider.setValue(value);
            provider.setSummary(provider.getEntry());
            updateCustomVisibility(PROVIDER_CUSTOM.equals(value),
                    customUrl, customKey, customModel);
            return true;
        });

        if (model != null) {
            final String modelValue = Settings.Global.getString(getContentResolver(), SETTING_MODEL);
            model.setValue(modelValue != null ? modelValue : "gemini-3.5-flash-lite");
            model.setSummary(model.getEntry());
            model.setOnPreferenceChangeListener((preference, newValue) -> {
                Settings.Global.putString(getContentResolver(), SETTING_MODEL, (String) newValue);
                model.setValue((String) newValue);
                model.setSummary(model.getEntry());
                return true;
            });
        }

        if (customUrl != null) {
            customUrl.setText(Settings.Global.getString(getContentResolver(), SETTING_CUSTOM_URL));
            customUrl.setOnPreferenceChangeListener((preference, newValue) -> {
                Settings.Global.putString(getContentResolver(), SETTING_CUSTOM_URL, (String) newValue);
                return true;
            });
        }
        if (customKey != null) {
            customKey.setText(Settings.Global.getString(getContentResolver(), SETTING_CUSTOM_KEY));
            customKey.setOnPreferenceChangeListener((preference, newValue) -> {
                Settings.Global.putString(getContentResolver(), SETTING_CUSTOM_KEY, (String) newValue);
                return true;
            });
        }
        if (customModel != null) {
            customModel.setText(Settings.Global.getString(getContentResolver(), SETTING_CUSTOM_MODEL));
            customModel.setOnPreferenceChangeListener((preference, newValue) -> {
                Settings.Global.putString(getContentResolver(), SETTING_CUSTOM_MODEL, (String) newValue);
                return true;
            });
        }

        updateCustomVisibility(PROVIDER_CUSTOM.equals(provider.getValue()),
                customUrl, customKey, customModel);
    }

    private void updateCustomVisibility(boolean visible, EditTextPreference url,
            EditTextPreference key, EditTextPreference model) {
        if (url != null) {
            url.setVisible(visible);
        }
        if (key != null) {
            key.setVisible(visible);
        }
        if (model != null) {
            model.setVisible(visible);
        }
    }

    private void initBehaviorPreferences() {
        bindSwitch(KEY_PRIVACY_ALERTS, SETTING_PRIVACY_ALERTS, 1);
        bindSwitch(KEY_AUTO_OTP, SETTING_AUTO_OTP, 1);
        bindSwitch(KEY_DEV_MODE, SETTING_DEV_MODE, 0);
    }

    private void bindSwitch(String key, String setting, int defaultValue) {
        final SwitchPreferenceCompat pref = findPreference(key);
        if (pref == null) {
            return;
        }
        pref.setChecked(Settings.Global.getInt(getContentResolver(), setting, defaultValue) == 1);
        pref.setOnPreferenceChangeListener((preference, newValue) -> {
            Settings.Global.putInt(getContentResolver(), setting, (Boolean) newValue ? 1 : 0);
            return true;
        });
    }

    private void initToolPreferences() {
        final PreferenceCategory category = findPreference(KEY_TOOLS_CATEGORY);
        if (category == null) {
            return;
        }
        final String[] tools = getResources().getStringArray(R.array.legacydroid_luminaai_tools);
        for (final String tool : tools) {
            final String key = Settings.Global.LUMINA_TOOL_ENABLED_PREFIX + tool;
            final SwitchPreferenceCompat pref = new SwitchPreferenceCompat(getPrefContext());
            pref.setKey(key);
            pref.setTitle(humanize(tool));
            pref.setSummary(tool);
            pref.setPersistent(false);
            pref.setChecked(Settings.Global.getInt(getContentResolver(), key, 1) == 1);
            pref.setOnPreferenceChangeListener((preference, newValue) -> {
                Settings.Global.putInt(getContentResolver(), key, (Boolean) newValue ? 1 : 0);
                return true;
            });
            category.addPreference(pref);
        }
    }

    private static String humanize(String name) {
        final String[] parts = name.split("_");
        final StringBuilder sb = new StringBuilder();
        for (final String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0)));
            sb.append(part.substring(1));
        }
        return sb.toString();
    }
}