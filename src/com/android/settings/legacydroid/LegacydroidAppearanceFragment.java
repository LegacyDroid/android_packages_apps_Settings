/*
 * SPDX-FileCopyrightText: 2026 The LegacyDroid Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.legacydroid;

import android.app.settings.SettingsEnums;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;

/** LegacyDroid appearance settings. */
public class LegacydroidAppearanceFragment extends DashboardFragment {

    private static final String TAG = "LegacydroidAppearance";

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
}