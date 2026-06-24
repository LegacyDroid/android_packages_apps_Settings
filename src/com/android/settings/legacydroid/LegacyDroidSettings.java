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
                    .setArguments(ProfileCustomizationFragment.createArgs(profileName, mActiveProfile))
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
        Context context = getContext();
        SharedPreferences custom = context.getSharedPreferences(PREFS_CUSTOM, 0);

        BatterySaverUtils.setPowerSaveMode(context, false, false);
        setMasterSync(true);
        resetRefreshRate(context);
        setGameDriver(context, false);
        setDnd(context, false);
        setAnimationScales(context, 1f);

        if (isActionEnabled(custom, profile, "action_stop_apps")) stopBackgroundApps(context);
        if (isActionEnabled(custom, profile, "action_pause_sync")) setMasterSync(false);
        if (isActionEnabled(custom, profile, "action_high_refresh")) setHighRefreshRate(context);
        if (isActionEnabled(custom, profile, "action_game_driver")) setGameDriver(context, true);
        if (isActionEnabled(custom, profile, "action_dnd")) setDnd(context, true);
        if (isActionEnabled(custom, profile, "action_low_refresh")) setLowRefreshRate(context);
        if (isActionEnabled(custom, profile, "action_disable_anim")) setAnimationScales(context, 0f);
        if (isActionEnabled(custom, profile, "action_battery_saver"))
            BatterySaverUtils.setPowerSaveMode(context, true, false);

        Toast.makeText(context,
                getString(R.string.legacy_droid_profile_applied,
                        getString(getTitleResForProfile(profile))),
                Toast.LENGTH_SHORT).show();
    }

    public static void applyAction(Context context, String action, boolean enable) {
        if (enable) {
            switch (action) {
                case "action_stop_apps": stopBackgroundApps(context); break;
                case "action_pause_sync": setMasterSync(false); break;
                case "action_high_refresh": setHighRefreshRate(context); break;
                case "action_game_driver": setGameDriver(context, true); break;
                case "action_dnd": setDnd(context, true); break;
                case "action_low_refresh": setLowRefreshRate(context); break;
                case "action_disable_anim": setAnimationScales(context, 0f); break;
                case "action_battery_saver":
                    BatterySaverUtils.setPowerSaveMode(context, true, false); break;
            }
        } else {
            switch (action) {
                case "action_pause_sync": setMasterSync(true); break;
                case "action_high_refresh":
                case "action_low_refresh": resetRefreshRate(context); break;
                case "action_game_driver": setGameDriver(context, false); break;
                case "action_dnd": setDnd(context, false); break;
                case "action_disable_anim": setAnimationScales(context, 1f); break;
                case "action_battery_saver":
                    BatterySaverUtils.setPowerSaveMode(context, false, false); break;
                case "action_stop_apps": break;
            }
        }
    }

    private boolean isActionEnabled(SharedPreferences prefs, String profile, String action) {
        return prefs.getBoolean(profile + "_" + action, getActionDefault(profile, action));
    }

    public static boolean getActionDefault(String profile, String action) {
        switch (profile) {
            case "performance":
                switch (action) {
                    case "action_stop_apps":
                    case "action_pause_sync":
                    case "action_high_refresh":
                    case "action_game_driver":
                    case "action_dnd":
                    case "action_disable_anim":
                        return true;
                    case "action_battery_saver":
                        return false;
                    default:
                        return false;
                }
            case "battery_saver":
                switch (action) {
                    case "action_pause_sync":
                    case "action_low_refresh":
                    case "action_dnd":
                    case "action_disable_anim":
                    case "action_battery_saver":
                        return true;
                    default:
                        return false;
                }
            default:
                return false;
        }
    }

    private static void setAnimationScales(Context context, float scale) {
        try {
            ContentResolver cr = context.getContentResolver();
            Settings.Global.putFloat(cr, ANIM_WINDOW, scale);
            Settings.Global.putFloat(cr, ANIM_TRANSITION, scale);
            Settings.Global.putFloat(cr, ANIM_ANIMATOR, scale);
        } catch (Exception ignored) {}
    }

    private static void stopBackgroundApps(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        if (packages == null) return;
        for (ApplicationInfo app : packages) {
            if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0
                    && !app.packageName.equals(context.getPackageName())) {
                try { am.forceStopPackage(app.packageName); } catch (Exception ignored) {}
            }
        }
    }

    private static void setMasterSync(boolean enabled) {
        ContentResolver.setMasterSyncAutomatically(enabled);
    }

    private static float getMaxRefreshRate(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) return 60f;
        float max = 60f;
        for (Display.Mode mode : wm.getDefaultDisplay().getSupportedModes()) {
            if (mode.getRefreshRate() > max) max = mode.getRefreshRate();
        }
        return max;
    }

    private static float getMinRefreshRate(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) return 60f;
        float min = Float.MAX_VALUE;
        for (Display.Mode mode : wm.getDefaultDisplay().getSupportedModes()) {
            if (mode.getRefreshRate() < min) min = mode.getRefreshRate();
        }
        return min < Float.MAX_VALUE ? min : 60f;
    }

    private static void setHighRefreshRate(Context context) {
        try {
            ContentResolver cr = context.getContentResolver();
            float rate = getMaxRefreshRate(context);
            Settings.System.putFloat(cr, Settings.System.MIN_REFRESH_RATE, rate);
            Settings.System.putFloat(cr, Settings.System.PEAK_REFRESH_RATE, rate);
        } catch (Exception ignored) {}
    }

    private static void setLowRefreshRate(Context context) {
        try {
            ContentResolver cr = context.getContentResolver();
            float rate = getMinRefreshRate(context);
            Settings.System.putFloat(cr, Settings.System.MIN_REFRESH_RATE, rate);
            Settings.System.putFloat(cr, Settings.System.PEAK_REFRESH_RATE, rate);
        } catch (Exception ignored) {}
    }

    private static void resetRefreshRate(Context context) {
        try {
            ContentResolver cr = context.getContentResolver();
            Settings.System.putFloat(cr, Settings.System.MIN_REFRESH_RATE, 60f);
            Settings.System.putFloat(cr, Settings.System.PEAK_REFRESH_RATE, 60f);
        } catch (Exception ignored) {}
    }

    private static void setGameDriver(Context context, boolean enabled) {
        try {
            Settings.Global.putString(context.getContentResolver(),
                    KEY_UPDATABLE_DRIVER, enabled ? "1" : "0");
        } catch (Exception ignored) {}
    }

    private static void setDnd(Context context, boolean on) {
        try {
            NotificationManager nm = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
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
