/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.deviceinfo.aboutphone;

import android.app.ActivityManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.profileselector.ProfileSelectFragment.ProfileType;
import com.android.settings.deviceinfo.storage.StorageCacheHelper;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.deviceinfo.PrivateStorageInfo;
import com.android.settingslib.deviceinfo.StorageManagerVolumeProvider;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.utils.ThreadUtils;
import com.android.settingslib.widget.LayoutPreference;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable
public class MyDeviceInfoFragment extends DashboardFragment {

    private static final String LOG_TAG = "MyDeviceInfoFragment";
    private static final String KEY_MY_DEVICE_INFO_HEADER = "my_device_info_header";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DEVICEINFO;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_uri_about;
    }

    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.my_device_info;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return new ArrayList<>();
    }

    @Override
    public void onStart() {
        super.onStart();
        initHeader();
    }

    @Override
    public void onResume() {
        super.onResume();
        bindHeaderViews();
    }

    private void initHeader() {
        final LayoutPreference headerPreference =
                getPreferenceScreen().findPreference(KEY_MY_DEVICE_INFO_HEADER);
        final boolean shouldDisplayHeader = getContext().getResources().getBoolean(
                R.bool.config_show_device_header_in_device_info);
        headerPreference.setVisible(shouldDisplayHeader);
        if (!shouldDisplayHeader) {
            return;
        }
        final View headerView = headerPreference.findViewById(R.id.about_device_header);

        final TextView modelName = headerView.findViewById(R.id.about_model_name);
        modelName.setText(Build.MODEL);

        final View deviceNameCard = headerView.findViewById(R.id.about_device_name_card);
        deviceNameCard.setOnClickListener(v -> openAboutDeviceMore());

        final View storageCard = headerView.findViewById(R.id.about_storage_card);
        storageCard.setOnClickListener(v -> {
            startActivity(new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS));
        });

        bindSpecs(headerView);
    }

    private void bindHeaderViews() {
        final LayoutPreference headerPreference =
                getPreferenceScreen().findPreference(KEY_MY_DEVICE_INFO_HEADER);
        final View headerView = headerPreference.findViewById(R.id.about_device_header);

        final TextView deviceNameValue = headerView.findViewById(R.id.about_device_name_value);
        String deviceName = Settings.Global.getString(getContext().getContentResolver(),
                Settings.Global.DEVICE_NAME);
        if (TextUtils.isEmpty(deviceName)) {
            deviceName = Build.MODEL;
        }
        deviceNameValue.setText(deviceName);

        bindStorageInfo(headerView);
    }

    private void bindStorageInfo(View headerView) {
        final TextView caption = headerView.findViewById(R.id.about_storage_caption);
        final ProgressBar bar = headerView.findViewById(R.id.about_storage_bar);
        final int userId = Utils.getCurrentUserIdOfType(
                getContext().getSystemService(android.os.UserManager.class),
                ProfileType.PERSONAL);
        final StorageCacheHelper storageCacheHelper = new StorageCacheHelper(getContext(), userId);

        long cachedUsedSize = storageCacheHelper.retrieveUsedSize();
        long cachedTotalSize = storageCacheHelper.retrieveCachedSize().totalSize;
        if (cachedUsedSize != 0 && cachedTotalSize != 0) {
            updateStorageInfo(caption, bar, cachedUsedSize, cachedTotalSize);
        }

        ThreadUtils.postOnBackgroundThread(() -> {
            final PrivateStorageInfo info = PrivateStorageInfo.getPrivateStorageInfo(
                    new StorageManagerVolumeProvider(
                            getContext().getSystemService(StorageManager.class)));
            long usedBytes = info.totalBytes - info.freeBytes;
            storageCacheHelper.cacheUsedSize(usedBytes);
            ThreadUtils.postOnMainThread(() ->
                    updateStorageInfo(caption, bar, usedBytes, info.totalBytes));
        });
    }

    private static void updateStorageInfo(
            TextView caption, ProgressBar bar, long usedBytes, long totalBytes) {
        if (totalBytes == 0L) {
            return;
        }
        final Context context = caption.getContext();
        bar.setProgress((int) (usedBytes * 100 / totalBytes));
        caption.setText(context.getString(R.string.about_storage_value_summary,
                Formatter.formatShortFileSize(context, usedBytes),
                Formatter.formatShortFileSize(context, totalBytes)));
    }

    private void bindSpecs(View headerView) {
        final TextView processorValue = headerView.findViewById(R.id.about_processor_value);
        final String socModel = Build.SOC_MODEL;
        processorValue.setText(!TextUtils.isEmpty(socModel)
                && !Build.UNKNOWN.equals(socModel) ? socModel : Build.HARDWARE);

        final TextView batteryValue = headerView.findViewById(R.id.about_battery_value);
        final double capacityMah =
                new com.android.internal.os.PowerProfile(getContext()).getBatteryCapacity();
        batteryValue.setText(getString(R.string.about_battery_value_summary,
                Math.round(capacityMah)));

        final TextView ramValue = headerView.findViewById(R.id.about_ram_value);
        final ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        final ActivityManager activityManager =
                getContext().getSystemService(ActivityManager.class);
        activityManager.getMemoryInfo(memoryInfo);
        ramValue.setText(String.format("%.1f GB", memoryInfo.totalMem / (1024.0 * 1024 * 1024)));
    }

    private void openAboutDeviceMore() {
        new com.android.settings.core.SubSettingLauncher(getContext())
                .setDestination(AboutDeviceMoreFragment.class.getName())
                .setSourceMetricsCategory(getMetricsCategory())
                .launch();
    }

    /**
     * For Search.
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.my_device_info);
}
