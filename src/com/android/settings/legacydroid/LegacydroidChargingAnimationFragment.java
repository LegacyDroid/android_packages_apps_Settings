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
import android.util.Base64;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.widget.SeekBarPreference;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/** LegacyDroid charging animation options. */
public class LegacydroidChargingAnimationFragment extends DashboardFragment {

    private static final String TAG = "LegacydroidChargingAnimation";

    private static final String KEY_MODE = "legacydroid_charging_animation";
    private static final String KEY_IMAGE = "legacydroid_charging_image";
    private static final String KEY_TRANSPARENCY = "legacydroid_charging_image_transparency";
    private static final String KEY_SIZE = "legacydroid_charging_image_size";

    private static final String SETTING_MODE = "legacydroid_charging_animation";
    private static final String SETTING_IMAGE = "legacydroid_charging_image";
    private static final String SETTING_IMAGE_DATA = "legacydroid_charging_image_data";
    private static final String SETTING_TRANSPARENCY = "legacydroid_charging_image_transparency";
    private static final String SETTING_SIZE = "legacydroid_charging_image_size";

    private static final String MODE_AOSP = "aosp";
    private static final String MODE_NONE = "none";
    private static final String MODE_CUSTOM = "custom";

    private static final int DEFAULT_TRANSPARENCY = 0;
    private static final int DEFAULT_SIZE = 100;
    private static final int MAX_ENCODED_BYTES = 22000;
    private static final int MAX_DECODE_DIMENSION = 2048;

    private ActivityResultLauncher<String[]> mPickImageLauncher;

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.legacydroid_charging_animation;
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
        // The XML summary "%s" is auto-formatted with the selected entry; do not set a
        // static summary here or it will stop reflecting future selections.
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
        // The media documents provider refuses plain URI grants (it requires access obtained
        // via ACTION_OPEN_DOCUMENT), so the image bytes are stored in Settings.Global instead.
        // Strings there are capped at 32KB, hence the JPEG is encoded to fit.
        final String imageData = encodeImageData(uri);
        if (imageData != null) {
            Settings.Global.putString(getContentResolver(), SETTING_IMAGE_DATA, imageData);
        }
        final Preference image = findPreference(KEY_IMAGE);
        if (image != null) {
            image.setSummary(getString(R.string.legacydroid_charging_image_picked_summary,
                    queryDisplayName(uri)));
        }
    }

    /**
     * Loads the image, downscales it, and returns a base64-encoded JPEG small enough for
     * {@link Settings.Global} (strings capped at 32KB). The MediaDocumentsProvider will not
     * serve its documents to SystemUI through a URI grant, so this is the only channel that
     * works without extra permissions.
     */
    private String encodeImageData(Uri uri) {
        try {
            final BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream stream = getContentResolver().openInputStream(uri)) {
                if (stream == null) {
                    return null;
                }
                BitmapFactory.decodeStream(stream, null, bounds);
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return null;
            }

            int sampleSize = 1;
            while (bounds.outWidth / sampleSize > MAX_DECODE_DIMENSION
                    || bounds.outHeight / sampleSize > MAX_DECODE_DIMENSION) {
                sampleSize *= 2;
            }

            final BitmapFactory.Options decode = new BitmapFactory.Options();
            decode.inSampleSize = sampleSize;
            Bitmap bitmap;
            try (InputStream stream = getContentResolver().openInputStream(uri)) {
                if (stream == null) {
                    return null;
                }
                bitmap = BitmapFactory.decodeStream(stream, null, decode);
            }
            if (bitmap == null) {
                return null;
            }

            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            int quality = 85;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
            while (out.size() > MAX_ENCODED_BYTES && quality > 40) {
                quality -= 10;
                out.reset();
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
            }
            while (out.size() > MAX_ENCODED_BYTES) {
                final Bitmap scaled = Bitmap.createScaledBitmap(bitmap,
                        Math.max(1, bitmap.getWidth() / 2),
                        Math.max(1, bitmap.getHeight() / 2), true);
                if (scaled != bitmap) {
                    bitmap.recycle();
                }
                bitmap = scaled;
                out.reset();
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
            }
            bitmap.recycle();
            final String encoded = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
            Log.i(TAG, "Charging image encoded: " + out.size() + " bytes");
            return encoded;
        } catch (Exception e) {
            Log.w(TAG, "Failed to encode charging image", e);
            return null;
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