package com.android.settings.legacydroid;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.fuelgauge.BatterySaverUtils;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class LegacyDroidSettings extends DashboardFragment {

    private static final String TAG = "LegacyDroidSettings";
    private static final String PREFS_NAME = "legacy_droid_prefs";
    private static final String KEY_SAVED_PROFILE = "saved_profile";
    private static final String PREFS_CUSTOM = "legacy_droid_customization";
    private static final String KEY_UPDATABLE_DRIVER = "updatable_driver_all_apps";

    private static final String ANIM_WINDOW = "window_animation_scale";
    private static final String ANIM_TRANSITION = "transition_animation_scale";
    private static final String ANIM_ANIMATOR = "animator_duration_scale";

    private static final String KEY_BALANCE = "profile_balance";
    private static final String KEY_PERFORMANCE = "profile_performance";
    private static final String KEY_BATTERY_SAVER = "profile_battery_saver";

    private String mActiveProfile = "balance";

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
        return R.xml.legacy_droid_settings;
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

        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, 0);
        mActiveProfile = prefs.getString(KEY_SAVED_PROFILE, "balance");

        String[] taglines = getResources().getStringArray(R.array.legacy_droid_taglines);
        String tagline = taglines[ThreadLocalRandom.current().nextInt(taglines.length)];
        getPreferenceScreen().setSummary(tagline);

        setupProfilePref(KEY_BALANCE, "balance");
        setupProfilePref(KEY_PERFORMANCE, "performance");
        setupProfilePref(KEY_BATTERY_SAVER, "battery_saver");
    }

    private void setupProfilePref(String prefKey, String profileName) {
        ProfilePreference pref = findPreference(prefKey);
        if (pref == null) return;

        boolean isActive = profileName.equals(mActiveProfile);
        pref.setSummary(getActiveSummary(profileName, isActive));

        pref.setOnPreferenceClickListener(p -> {
            mActiveProfile = profileName;
            applyProfile(profileName);
            getContext().getSharedPreferences(PREFS_NAME, 0)
                    .edit().putString(KEY_SAVED_PROFILE, profileName).apply();
            updateSummaries();
            return true;
        });

        pref.setGearClickListener(v -> {
            new SubSettingLauncher(getContext())
                    .setDestination(ProfileCustomizationFragment.class.getName())
                    .setArguments(ProfileCustomizationFragment.createArgs(profileName))
                    .setTitleRes(getTitleResForProfile(profileName))
                    .setSourceMetricsCategory(getMetricsCategory())
                    .launch();
        });
    }

    private int getTitleResForProfile(String profile) {
        switch (profile) {
            case "performance": return R.string.legacy_droid_profile_performance;
            case "battery_saver": return R.string.legacy_droid_profile_battery_saver;
            default: return R.string.legacy_droid_profile_balance;
        }
    }

    private String getActiveSummary(String profile, boolean isActive) {
        int summaryRes;
        switch (profile) {
            case "performance": summaryRes = R.string.legacy_droid_summary_performance; break;
            case "battery_saver": summaryRes = R.string.legacy_droid_summary_battery_saver; break;
            default: summaryRes = R.string.legacy_droid_summary_balance; break;
        }
        String summary = getString(summaryRes);
        if (isActive) {
            summary = getString(R.string.legacy_droid_active_prefix, summary);
        }
        return summary;
    }

    private void updateSummaries() {
        setupProfilePref(KEY_BALANCE, "balance");
        setupProfilePref(KEY_PERFORMANCE, "performance");
        setupProfilePref(KEY_BATTERY_SAVER, "battery_saver");
    }

    private void applyProfile(String profile) {
        SharedPreferences custom = getContext().getSharedPreferences(PREFS_CUSTOM, 0);

        switch (profile) {
            case "performance":
                BatterySaverUtils.setPowerSaveMode(getContext(), false, false);
                if (custom.getBoolean(profile + "_action_stop_apps", true)) stopBackgroundApps();
                if (custom.getBoolean(profile + "_action_pause_sync", true)) setMasterSync(false);
                if (custom.getBoolean(profile + "_action_high_refresh", true)) setHighRefreshRate();
                if (custom.getBoolean(profile + "_action_game_driver", true)) setGameDriver(true);
                if (custom.getBoolean(profile + "_action_dnd", true)) setDnd(true);
                setAnimationScales(0f);
                break;

            case "battery_saver":
                BatterySaverUtils.setPowerSaveMode(getContext(), true, false);
                if (custom.getBoolean(profile + "_action_low_refresh", true)) setLowRefreshRate();
                setMasterSync(true);
                setGameDriver(false);
                setDnd(false);
                setAnimationScales(0f);
                break;

            case "balance":
            default:
                BatterySaverUtils.setPowerSaveMode(getContext(), false, false);
                setMasterSync(true);
                resetRefreshRate();
                setGameDriver(false);
                setDnd(false);
                setAnimationScales(1f);
                break;
        }

        Toast.makeText(getContext(),
                getString(R.string.legacy_droid_profile_applied,
                        getString(getTitleResForProfile(profile))),
                Toast.LENGTH_SHORT).show();
    }

    private void setAnimationScales(float scale) {
        try {
            Settings.Global.putFloat(getContentResolver(), ANIM_WINDOW, scale);
            Settings.Global.putFloat(getContentResolver(), ANIM_TRANSITION, scale);
            Settings.Global.putFloat(getContentResolver(), ANIM_ANIMATOR, scale);
        } catch (Exception ignored) {}
    }

    private void stopBackgroundApps() {
        Context ctx = getContext();
        if (ctx == null) return;
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        PackageManager pm = ctx.getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        if (packages == null) return;
        for (ApplicationInfo app : packages) {
            if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0
                    && !app.packageName.equals(ctx.getPackageName())) {
                try { am.forceStopPackage(app.packageName); } catch (Exception ignored) {}
            }
        }
    }

    private void setMasterSync(boolean enabled) {
        ContentResolver.setMasterSyncAutomatically(enabled);
    }

    private float getMaxRefreshRate() {
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) return 60f;
        float max = 60f;
        for (Display.Mode mode : wm.getDefaultDisplay().getSupportedModes()) {
            if (mode.getRefreshRate() > max) max = mode.getRefreshRate();
        }
        return max;
    }

    private float getMinRefreshRate() {
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) return 60f;
        float min = Float.MAX_VALUE;
        for (Display.Mode mode : wm.getDefaultDisplay().getSupportedModes()) {
            if (mode.getRefreshRate() < min) min = mode.getRefreshRate();
        }
        return min < Float.MAX_VALUE ? min : 60f;
    }

    private void setHighRefreshRate() {
        try {
            float rate = getMaxRefreshRate();
            Settings.System.putFloat(getContentResolver(), Settings.System.MIN_REFRESH_RATE, rate);
            Settings.System.putFloat(getContentResolver(), Settings.System.PEAK_REFRESH_RATE, rate);
        } catch (Exception ignored) {}
    }

    private void setLowRefreshRate() {
        try {
            float rate = getMinRefreshRate();
            Settings.System.putFloat(getContentResolver(), Settings.System.MIN_REFRESH_RATE, rate);
            Settings.System.putFloat(getContentResolver(), Settings.System.PEAK_REFRESH_RATE, rate);
        } catch (Exception ignored) {}
    }

    private void resetRefreshRate() {
        try {
            Settings.System.putFloat(getContentResolver(), Settings.System.MIN_REFRESH_RATE, 60f);
            Settings.System.putFloat(getContentResolver(), Settings.System.PEAK_REFRESH_RATE, 60f);
        } catch (Exception ignored) {}
    }

    private void setGameDriver(boolean enabled) {
        try {
            Settings.Global.putString(getContentResolver(),
                    KEY_UPDATABLE_DRIVER, enabled ? "1" : "0");
        } catch (Exception ignored) {}
    }

    private void setDnd(boolean on) {
        try {
            NotificationManager nm = (NotificationManager)
                    getContext().getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            try {
                nm.setInterruptionFilter(on
                        ? NotificationManager.INTERRUPTION_FILTER_ALARMS
                        : NotificationManager.INTERRUPTION_FILTER_ALL);
            } catch (SecurityException e) {
                try {
                    Object sbn = NotificationManager.class.getMethod("getService").invoke(null);
                    sbn.getClass().getMethod("setZenMode", int.class, Object.class, String.class)
                            .invoke(sbn, on ? 1 : 0, null, "LegacyDroid");
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.legacy_droid_settings;
                    return Arrays.asList(sir);
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return new ArrayList<>();
                }
            };
}
