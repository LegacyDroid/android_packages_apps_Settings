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
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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

    private static final int MODEL_FETCH_CONNECT_MS = 5_000;
    private static final int MODEL_FETCH_READ_MS = 8_000;

    /** Bumped to drop stale results when the endpoint or provider changes. */
    private int mModelFetchGeneration;

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
        provider.setValue(providerValue != null ? providerValue : "lumina");
        provider.setSummary(provider.getEntry());
        provider.setOnPreferenceChangeListener((preference, newValue) -> {
            final String value = (String) newValue;
            Settings.Global.putString(getContentResolver(), SETTING_PROVIDER, value);
            provider.setValue(value);
            provider.setSummary(provider.getEntry());
            updateCustomVisibility(PROVIDER_CUSTOM.equals(value),
                    customUrl, customKey, customModel);
            if (PROVIDER_CUSTOM.equals(value)) {
                fetchModelCatalog(model);
            } else {
                applyStaticModelEntries(model);
            }
            return true;
        });

        if (model != null) {
            final String modelValue = Settings.Global.getString(getContentResolver(), SETTING_MODEL);
            model.setValue(modelValue != null ? modelValue : "gemini-3.7-flash");
            model.setSummary(model.getEntry());
            model.setOnPreferenceChangeListener((preference, newValue) -> {
                final String value = (String) newValue;
                Settings.Global.putString(getContentResolver(), SETTING_MODEL, value);
                if (PROVIDER_CUSTOM.equals(provider.getValue())) {
                    // Mirror into the custom model slot: LumiApi reads that
                    // one first, so a stale typed id must never shadow the
                    // id just picked from the fetched catalog.
                    Settings.Global.putString(getContentResolver(), SETTING_CUSTOM_MODEL, value);
                    if (customModel != null) {
                        customModel.setText(value);
                    }
                }
                model.setValue(value);
                model.setSummary(model.getEntry());
                return true;
            });
        }

        if (customUrl != null) {
            customUrl.setText(Settings.Global.getString(getContentResolver(), SETTING_CUSTOM_URL));
            customUrl.setOnPreferenceChangeListener((preference, newValue) -> {
                Settings.Global.putString(getContentResolver(), SETTING_CUSTOM_URL, (String) newValue);
                if (PROVIDER_CUSTOM.equals(provider.getValue())) {
                    fetchModelCatalog(model);
                }
                return true;
            });
        }
        if (customKey != null) {
            customKey.setText(Settings.Global.getString(getContentResolver(), SETTING_CUSTOM_KEY));
            customKey.setOnPreferenceChangeListener((preference, newValue) -> {
                Settings.Global.putString(getContentResolver(), SETTING_CUSTOM_KEY, (String) newValue);
                if (PROVIDER_CUSTOM.equals(provider.getValue())) {
                    fetchModelCatalog(model);
                }
                return true;
            });
        }
        if (customModel != null) {
            customModel.setText(Settings.Global.getString(getContentResolver(), SETTING_CUSTOM_MODEL));
            customModel.setOnPreferenceChangeListener((preference, newValue) -> {
                final String value = ((String) newValue).trim();
                Settings.Global.putString(getContentResolver(), SETTING_CUSTOM_MODEL, value);
                Settings.Global.putString(getContentResolver(), SETTING_MODEL, value);
                if (model != null) {
                    model.setValue(value);
                    model.setSummary(model.getEntry() != null ? model.getEntry() : value);
                }
                return true;
            });
        }

        final boolean isCustom = PROVIDER_CUSTOM.equals(provider.getValue());
        updateCustomVisibility(isCustom, customUrl, customKey, customModel);
        if (isCustom) {
            fetchModelCatalog(model);
        } else {
            applyStaticModelEntries(model);
        }
    }

    private void applyStaticModelEntries(final ListPreference model) {
        // Invalidate any in-flight catalog fetch so a stale custom result
        // cannot land after the user switched back to Lumina Cloud.
        mModelFetchGeneration++;
        if (model == null) {
            return;
        }
        model.setEntries(R.array.legacydroid_luminaai_model_entries);
        model.setEntryValues(R.array.legacydroid_luminaai_model_values);
        keepModelSummary(model);
    }

    private void keepModelSummary(final ListPreference model) {
        final String current = Settings.Global.getString(getContentResolver(), SETTING_MODEL);
        if (current != null) {
            model.setValue(current);
        }
        model.setSummary(model.getEntry() != null ? model.getEntry() : model.getValue());
    }

    /**
     * Pull the OpenAI-compatible {@code GET /v1/models} catalog so the user
     * picks a model instead of typing its id. Falls back to the static list
     * when the endpoint is unreachable or malformed.
     */
    private void fetchModelCatalog(final ListPreference model) {
        if (model == null) {
            return;
        }
        final String baseUrl = Settings.Global.getString(getContentResolver(), SETTING_CUSTOM_URL);
        final String apiKey = Settings.Global.getString(getContentResolver(), SETTING_CUSTOM_KEY);
        // Bump first: even an early return (blank URL) must orphan results
        // fetched for the previous endpoint.
        final int generation = ++mModelFetchGeneration;
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            return;
        }
        new Thread(() -> {
            final List<String> ids = fetchModelIds(baseUrl.trim(), apiKey);
            if (ids.isEmpty() || generation != mModelFetchGeneration) {
                return;
            }
            // Post to the main looper instead of requireActivity(): the
            // fragment can detach while the request is in flight.
            new Handler(Looper.getMainLooper()).post(() -> {
                if (generation != mModelFetchGeneration || !isAdded()) {
                    return;
                }
                model.setEntries(ids.toArray(new String[0]));
                model.setEntryValues(ids.toArray(new String[0]));
                final String current = Settings.Global.getString(getContentResolver(), SETTING_MODEL);
                if (current != null && !ids.contains(current)) {
                    // Catalog no longer lists the saved model: keep it visible
                    // so the summary does not go blank, then let the user pick.
                    final List<String> extended = new ArrayList<>(ids);
                    extended.add(0, current);
                    model.setEntries(extended.toArray(new String[0]));
                    model.setEntryValues(extended.toArray(new String[0]));
                }
                keepModelSummary(model);
            });
        }, "LuminaModelCatalog").start();
    }

    private static List<String> fetchModelIds(String baseUrl, String apiKey) {
        String endpoint = baseUrl;
        while (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        if (!endpoint.endsWith("/models")) {
            endpoint = endpoint.endsWith("/v1")
                    ? endpoint + "/models"
                    : endpoint + "/v1/models";
        }
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(endpoint).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(MODEL_FETCH_CONNECT_MS);
            conn.setReadTimeout(MODEL_FETCH_READ_MS);
            if (apiKey != null && !apiKey.isBlank()) {
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            }
            final int code = conn.getResponseCode();
            if (code != 200) {
                Log.w(TAG, "Model catalog fetch failed: HTTP " + code);
                return List.of();
            }
            final String raw;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                final StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                raw = sb.toString();
            }
            final JSONArray data = new JSONObject(raw).optJSONArray("data");
            if (data == null) {
                return List.of();
            }
            final List<String> ids = new ArrayList<>();
            for (int i = 0; i < data.length(); i++) {
                final String id = data.optJSONObject(i) != null
                        ? data.optJSONObject(i).optString("id")
                        : data.optString(i);
                if (!id.isBlank() && !ids.contains(id)) {
                    ids.add(id);
                }
            }
            return ids;
        } catch (Exception e) {
            Log.w(TAG, "Model catalog fetch failed", e);
            return List.of();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
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