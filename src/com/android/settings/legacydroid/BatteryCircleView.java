package com.android.settings.legacydroid;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.android.settings.R;

public class BatteryCircleView extends View {

    private static final int START_ANGLE = -90;
    private static final int FULL_CIRCLE = 360;
    private static final long ANIM_DURATION_MS = 1000;

    private float mStrokeWidth;
    private float mAnimatedPercent;
    private float mTargetPercent;
    private boolean mIsCharging;

    private final Paint mBackgroundPaint;
    private final Paint mArcPaint;
    private final Paint mTextPaint;
    private final Paint mSubTextPaint;

    private ValueAnimator mAnimator;
    private String mPercentText;
    private String mChargingText;

    private int mColorGood;
    private int mColorMaybe;
    private int mColorBad;
    private int mColorCharging;

    public BatteryCircleView(Context context) {
        this(context, null);
    }

    public BatteryCircleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BatteryCircleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mStrokeWidth = getResources().getDimensionPixelSize(R.dimen.storage_donut_thickness);
        mChargingText = context.getString(R.string.legacy_droid_charging);
        int textColor = resolveColorAttr(context, android.R.attr.textColorPrimary);

        mColorGood = context.getColor(R.color.battery_good_color_light);
        mColorMaybe = context.getColor(R.color.battery_maybe_color_light);
        mColorBad = context.getColor(R.color.battery_bad_color_light);
        mColorCharging = context.getColor(R.color.homepage_battery_background);

        mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBackgroundPaint.setStyle(Paint.Style.STROKE);
        mBackgroundPaint.setStrokeWidth(mStrokeWidth);
        mBackgroundPaint.setStrokeCap(Paint.Cap.ROUND);
        mBackgroundPaint.setColor(0x1A000000);

        mArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mArcPaint.setStyle(Paint.Style.STROKE);
        mArcPaint.setStrokeWidth(mStrokeWidth);
        mArcPaint.setStrokeCap(Paint.Cap.ROUND);

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        mTextPaint.setColor(textColor);

        mSubTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSubTextPaint.setTextAlign(Paint.Align.CENTER);
        mSubTextPaint.setTypeface(Typeface.DEFAULT);
        mSubTextPaint.setColor(textColor);
        mSubTextPaint.setAlpha(138);
    }

    public void setPercent(float percent, boolean isCharging) {
        mTargetPercent = Math.max(0, Math.min(100, percent));
        mIsCharging = isCharging;
        java.text.NumberFormat nf = java.text.NumberFormat.getPercentInstance();
        nf.setMinimumFractionDigits(0);
        mPercentText = nf.format(mTargetPercent / 100.0);
        startAnimation();
    }

    private void startAnimation() {
        if (mAnimator != null) {
            mAnimator.cancel();
        }
        mAnimator = ValueAnimator.ofFloat(0, mTargetPercent);
        mAnimator.setDuration(ANIM_DURATION_MS);
        mAnimator.setInterpolator(new DecelerateInterpolator());
        mAnimator.addUpdateListener(animation -> {
            mAnimatedPercent = (float) animation.getAnimatedValue();
            invalidate();
        });
        mAnimator.start();
    }

    private int getArcColor() {
        if (mIsCharging) {
            return mColorCharging;
        }
        if (mTargetPercent < 20) {
            return mColorBad;
        } else if (mTargetPercent <= 75) {
            return mColorMaybe;
        } else {
            return mColorGood;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float radius = Math.min(centerX, centerY) - mStrokeWidth / 2f;

        canvas.drawCircle(centerX, centerY, radius, mBackgroundPaint);

        mArcPaint.setColor(getArcColor());
        float sweepAngle = (mAnimatedPercent / 100f) * FULL_CIRCLE;
        canvas.drawArc(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius,
                START_ANGLE,
                sweepAngle,
                false,
                mArcPaint
        );

        if (mPercentText == null) return;

        float textSize = radius * 0.5f;
        mTextPaint.setTextSize(textSize);

        if (mIsCharging) {
            float subTextSize = radius * 0.22f;
            mSubTextPaint.setTextSize(subTextSize);
            float totalHeight = getTextHeight(mTextPaint) + 4f + getTextHeight(mSubTextPaint);
            float blockTop = centerY - totalHeight / 2f;
            float pctY = blockTop - mTextPaint.ascent();
            canvas.drawText(mPercentText, centerX, pctY, mTextPaint);
            float chargeY = pctY + getTextHeight(mTextPaint) + 4f - mSubTextPaint.ascent();
            canvas.drawText(mChargingText, centerX, chargeY, mSubTextPaint);
        } else {
            float textY = centerY - (mTextPaint.ascent() + mTextPaint.descent()) / 2f;
            canvas.drawText(mPercentText, centerX, textY, mTextPaint);
        }
    }

    private static float getTextHeight(Paint paint) {
        return paint.descent() - paint.ascent();
    }

    private static int resolveColorAttr(Context context, int attr) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }
}
