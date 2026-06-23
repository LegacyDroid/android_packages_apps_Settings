package com.android.settings.legacydroid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

public class BatteryStatusPreference extends Preference {

    private BatteryCircleView mBatteryCircle;
    private TextView mCpuTempText;
    private TextView mBatteryTempText;
    private TextView mCpuNameText;
    private TextView mGpuNameText;

    private final BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateBatteryInfo(intent);
        }
    };

    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    private boolean mBound;

    public BatteryStatusPreference(Context context) {
        this(context, null);
    }

    public BatteryStatusPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.pref_battery_status);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        mBatteryCircle = (BatteryCircleView) view.findViewById(R.id.battery_circle);
        mCpuTempText = (TextView) view.findViewById(R.id.cpu_temp);
        mBatteryTempText = (TextView) view.findViewById(R.id.battery_temp);
        mCpuNameText = (TextView) view.findViewById(R.id.cpu_name);
        mGpuNameText = (TextView) view.findViewById(R.id.gpu_name);

        loadHardwareInfo();
        registerBatteryReceiver();
        mBound = true;
    }

    @Override
    public void onDetached() {
        super.onDetached();
        mBound = false;
        try {
            getContext().unregisterReceiver(mBatteryReceiver);
        } catch (Exception ignored) {
        }
    }

    private void registerBatteryReceiver() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        getContext().registerReceiver(mBatteryReceiver, filter);
    }

    private void updateBatteryInfo(Intent intent) {
        if (intent == null) return;

        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        float percent = (level / (float) scale) * 100f;

        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL;

        int tempRaw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
        float batteryTempC = tempRaw / 10f;

        if (mBatteryCircle != null) {
            mBatteryCircle.setPercent(percent, isCharging);
        }
        if (mBatteryTempText != null) {
            Context ctx = getContext();
            String tempStr = String.format("%.1f°C", batteryTempC);
            mBatteryTempText.setText(ctx.getString(R.string.legacy_droid_battery_temp, tempStr));
        }
    }

    private void loadHardwareInfo() {
        Context ctx = getContext();
        if (ctx == null) return;
        final Context appCtx = ctx.getApplicationContext();

        new Thread(() -> {
            String cpuName = CpuGpuUtils.getCpuName();
            String cpuTemp = CpuGpuUtils.getCpuTemp(appCtx);
            String gpuName = CpuGpuUtils.getGpuRenderer();

            mMainHandler.post(() -> {
                Context postCtx = getContext();
                if (postCtx == null || !mBound) return;

                if (cpuTemp != null && mCpuTempText != null) {
                    String tempStr = postCtx.getString(R.string.legacy_droid_temp_format, cpuTemp);
                    mCpuTempText.setText(postCtx.getString(R.string.legacy_droid_cpu_temp, tempStr));
                } else if (mCpuTempText != null) {
                    mCpuTempText.setVisibility(View.GONE);
                }

                if (cpuName != null && mCpuNameText != null) {
                    mCpuNameText.setText(postCtx.getString(R.string.legacy_droid_cpu_label, cpuName));
                } else if (mCpuNameText != null) {
                    mCpuNameText.setText(postCtx.getString(R.string.legacy_droid_cpu_label,
                            postCtx.getString(R.string.legacy_droid_unknown)));
                }

                if (gpuName != null && mGpuNameText != null) {
                    mGpuNameText.setText(postCtx.getString(R.string.legacy_droid_gpu_label, gpuName));
                } else if (mGpuNameText != null) {
                    mGpuNameText.setText(postCtx.getString(R.string.legacy_droid_gpu_label,
                            postCtx.getString(R.string.legacy_droid_unknown)));
                }
            });
        }).start();
    }
}
