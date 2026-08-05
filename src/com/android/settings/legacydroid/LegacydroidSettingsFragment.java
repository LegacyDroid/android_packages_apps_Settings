/*
 * SPDX-FileCopyrightText: 2026 The LegacyDroid Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.legacydroid;

import android.app.settings.SettingsEnums;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;

/** LegacyDroid settings: charging and LegacyDroid extras. */
public class LegacydroidSettingsFragment extends DashboardFragment {

    private static final String TAG = "LegacydroidSettings";

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.legacydroid_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_HOMEPAGE;
    }
}