package com.android.settings.legacydroid;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

public class ProfilePreference extends Preference {

    private View.OnClickListener mGearClickListener;

    public ProfilePreference(Context context) {
        this(context, null);
    }

    public ProfilePreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ProfilePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWidgetLayoutResource(R.layout.preference_widget_gear);
    }

    public void setGearClickListener(View.OnClickListener listener) {
        mGearClickListener = listener;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        View gear = view.findViewById(R.id.settings_button);
        if (gear != null) {
            gear.setOnClickListener(mGearClickListener);
        }
    }
}
