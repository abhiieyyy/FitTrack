package com.example.fittrack.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.core.content.ContextCompat;

import com.example.fittrack.R;

public class RingView extends View {

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bigPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint smallPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();

    private float shown = 0f;
    private String bigText = "0";
    private String smallText = "";
    private ValueAnimator animator;

    public RingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setColor(0x26FFFFFF);          // faint translucent white, readable on dark

        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);
        arcPaint.setColor(ContextCompat.getColor(context, R.color.accent_b));

        bigPaint.setColor(ContextCompat.getColor(context, R.color.text_primary));
        bigPaint.setTextAlign(Paint.Align.CENTER);
        bigPaint.setTypeface(Typeface.DEFAULT_BOLD);

        smallPaint.setColor(ContextCompat.getColor(context, R.color.text_secondary));
        smallPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setRingColor(int color) {
        arcPaint.setColor(color);
        invalidate();
    }

    public void setCenterText(String big, String small) {
        bigText = big;
        smallText = small;
        invalidate();
    }

    /** progress from 0 to 1, animated */
    public void setProgress(float p) {
        float target = Math.max(0f, Math.min(1f, p));
        if (animator != null) animator.cancel();
        animator = ValueAnimator.ofFloat(shown, target);
        animator.setDuration(900);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> {
            shown = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) animator.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        float size = Math.min(w, h);
        float stroke = size * 0.09f;
        trackPaint.setStrokeWidth(stroke);
        arcPaint.setStrokeWidth(stroke);

        float pad = stroke / 2f + 2f;
        oval.set((w - size) / 2f + pad, (h - size) / 2f + pad,
                (w + size) / 2f - pad, (h + size) / 2f - pad);

        canvas.drawOval(oval, trackPaint);
        if (shown > 0f) canvas.drawArc(oval, -90f, 360f * shown, false, arcPaint);

        bigPaint.setTextSize(size * 0.20f);
        smallPaint.setTextSize(size * 0.095f);
        float cx = w / 2f, cy = h / 2f;
        canvas.drawText(bigText, cx, cy, bigPaint);
        canvas.drawText(smallText, cx, cy + size * 0.15f, smallPaint);
    }
}