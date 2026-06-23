package com.android.settings.legacydroid;

import android.content.Context;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import java.util.concurrent.ThreadLocalRandom;

public class LegacyDroidTopLevelController extends BasePreferenceController {

    public LegacyDroidTopLevelController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean useDynamicSliceSummary() {
        return true;
    }

    @Override
    public CharSequence getSummary() {
        String[] taglines = mContext.getResources()
                .getStringArray(R.array.legacy_droid_taglines);
        return taglines[ThreadLocalRandom.current().nextInt(taglines.length)];
    }
}
