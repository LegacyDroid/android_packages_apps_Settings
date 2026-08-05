/*
 * SPDX-FileCopyrightText: 2026 The LegacyDroid Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.legacydroid;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.widget.SeekBarPreference;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/** LegacyDroid appearance settings: charging animation options. */
public class LegacydroidAppearanceFragment extends DashboardFragment {

    private static final String TAG = "LegacydroidAppearance";

    private static final String KEY_MODE = "legacydroid_charging_animation";
    private static final String KEY_IMAGE = "legacydroid_charging_image";
    private static final String KEY_TRANSPARENCY = "legacydroid_charging_image_transparency";
    private static final String KEY_SIZE = "legacydroid_charging_image_size";

    private static final String SETTING_MODE = "legacydroid_charging_animation";
    private static final String SETTING_IMAGE = "legacydroid_charging_image";
    private static final String SETTING_TRANSPARENCY = "legacydroid_charging_image_transparency";
    private static final String SETTING_SIZE = "legacydroid_charging_image_size";
    private static final String LEGACY_SETTING_IMAGE_DATA = "legacydroid_charging_image_data";

    private static final String IMAGE_FILE = "/data/system/legacydroid_charging_image.png";

    private static final String MODE_AOSP = "aosp";
    private static final String MODE_NONE = "none";
    private static final String MODE_CUSTOM = "custom";

    private static final int DEFAULT_TRANSPARENCY = 0;
    private static final int DEFAULT_SIZE = 100;
    private static final int MAX_IMAGE_DIMENSION = 1024;

    private ActivityResultLauncher<String[]> mPickImageLauncher;

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
    public void onAttach(Context context) {
        super.onAttach(context);
        mPickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(), this::onImagePicked);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        Settings.Global.putString(getContentResolver(), LEGACY_SETTING_IMAGE_DATA, null);
        initModePreference();
        initImagePreference();
        initSeekBar(KEY_TRANSPARENCY, SETTING_TRANSPARENCY, DEFAULT_TRANSPARENCY);
        initSeekBar(KEY_SIZE, SETTING_SIZE, DEFAULT_SIZE);
        updateModeUi(getCurrentMode());
    }

    private void initModePreference() {
        final ListPreference mode = findPreference(KEY_MODE);
        if (mode == null) {
            return;
        }
        mode.setValue(getCurrentMode());
        mode.setSummary(mode.getEntry());
        mode.setOnPreferenceChangeListener((preference, newValue) -> {
            final String value = (String) newValue;
            Settings.Global.putString(getContentResolver(), SETTING_MODE, value);
            updateModeUi(value);
            return true;
        });
    }

    private void initImagePreference() {
        final Preference image = findPreference(KEY_IMAGE);
        if (image == null) {
            return;
        }
        final Uri uri = getStoredImageUri();
        if (uri != null) {
            image.setSummary(getString(R.string.legacydroid_charging_image_picked_summary,
                    queryDisplayName(uri)));
        }
        image.setOnPreferenceClickListener(preference -> {
            mPickImageLauncher.launch(new String[]{"image/*"});
            return true;
        });
    }

    private void initSeekBar(String key, String setting, int defaultValue) {
        final SeekBarPreference seekBar = findPreference(key);
        if (seekBar == null) {
            return;
        }
        seekBar.setProgress(Settings.Global.getInt(getContentResolver(), setting, defaultValue));
        seekBar.setOnPreferenceChangeListener((preference, newValue) -> {
            Settings.Global.putInt(getContentResolver(), setting, (Integer) newValue);
            return true;
        });
    }

    private void onImagePicked(Uri uri) {
        if (uri == null) {
            return;
        }
        try {
            getContentResolver().takePersistableUriPermission(uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException e) {
            // Provider does not support persistable grants; fall back to temporary grant.
        }
        Settings.Global.putString(getContentResolver(), SETTING_IMAGE, uri.toString());
        saveImageFile(uri);
        final Preference image = findPreference(KEY_IMAGE);
        if (image != null) {
            image.setSummary(getString(R.string.legacydroid_charging_image_picked_summary,
                    queryDisplayName(uri)));
        }
    }

    /**
     * Loads the image (downscaled to at most {@link #MAX_IMAGE_DIMENSION} px) and writes it as a
     * PNG to {@link #IMAGE_FILE}, which SystemUI reads directly. Settings.Global strings are
     * limited to 32KB, so the image is passed via the file instead of the settings store.
     */
    private void saveImageFile(Uri uri) {
        try {
            final BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream stream = getContentResolver().openInputStream(uri)) {
                if (stream == null) {
                    return;
                }
                BitmapFactory.decodeStream(stream, null, bounds);
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return;
            }

            int sampleSize = 1;
            while (bounds.outWidth / (sampleSize * 2) >= MAX_IMAGE_DIMENSION
                    || bounds.outHeight / (sampleSize * 2) >= MAX_IMAGE_DIMENSION) {
                sampleSize *= 2;
            }

            final BitmapFactory.Options decode = new BitmapFactory.Options();
            decode.inSampleSize = sampleSize;
            Bitmap bitmap;
            try (InputStream stream = getContentResolver().openInputStream(uri)) {
                if (stream == null) {
                    return;
                }
                bitmap = BitmapFactory.decodeStream(stream, null, decode);
            }
            if (bitmap == null) {
                return;
            }

            final float scale = Math.min(1f,
                    Math.min((float) MAX_IMAGE_DIMENSION / bitmap.getWidth(),
                            (float) MAX_IMAGE_DIMENSION / bitmap.getHeight()));
            if (scale < 1f) {
                final Bitmap scaled = Bitmap.createScaledBitmap(bitmap,
                        Math.round(bitmap.getWidth() * scale),
                        Math.round(bitmap.getHeight() * scale), true);
                if (scaled != bitmap) {
                    bitmap.recycle();
                }
                bitmap = scaled;
            }

            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            bitmap.recycle();
            try (FileOutputStream file = new FileOutputStream(IMAGE_FILE)) {
                file.write(out.toByteArray());
            }
            new File(IMAGE_FILE).setReadable(true, false);
        } catch (Exception e) {
            // Keep the previous image if writing fails.
        }
    }

    private void updateModeUi(String mode) {
        final boolean custom = MODE_CUSTOM.equals(mode);
        final Preference image = findPreference(KEY_IMAGE);
        if (image != null) {
            image.setVisible(custom);
        }
        final SeekBarPreference transparency = findPreference(KEY_TRANSPARENCY);
        if (transparency != null) {
            transparency.setVisible(custom);
        }
        final SeekBarPreference size = findPreference(KEY_SIZE);
        if (size != null) {
            size.setVisible(custom);
        }
    }

    private String getCurrentMode() {
        String mode = Settings.Global.getString(getContentResolver(), SETTING_MODE);
        if (mode == null) {
            mode = MODE_AOSP;
        }
        return mode;
    }

    private Uri getStoredImageUri() {
        final String uri = Settings.Global.getString(getContentResolver(), SETTING_IMAGE);
        return uri != null ? Uri.parse(uri) : null;
    }

    private String queryDisplayName(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    return cursor.getString(index);
                }
            }
        } catch (Exception e) {
            // Fall back to the URI's last path segment.
        }
        final String lastSegment = uri.getLastPathSegment();
        return lastSegment != null ? lastSegment : uri.toString();
    }
}