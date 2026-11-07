package com.example.fittrack.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class GlowRingView extends View {
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint big = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint small = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float d = getResources().getDisplayMetrics().density;

    private int value, goal = 1, shownValue;
    private float shown;
    private String label = "";
    private int c1 = 0xFF7C5CFF, c2 = 0xFF00E0B8;
    private ValueAnimator anim;

    public GlowRingView(Context c, AttributeSet a) {
        super(c, a);
        setLayerType(LAYER_TYPE_SOFTWARE, null);   // needed for blur glow
        float stroke = 11 * d;
        for (Paint p : new Paint[]{track, arc, glow}) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(stroke);
            p.setStrokeCap(Paint.Cap.ROUND);
        }
        track.setColor(0x1AFFFFFF);
        glow.setMaskFilter(new BlurMaskFilter(12 * d, BlurMaskFilter.Blur.NORMAL));
        glow.setAlpha(150);
        big.setColor(0xFFF2F4FA);
        big.setTextSize(28 * d);
        big.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        big.setTextAlign(Paint.Align.CENTER);
        small.setColor(0xFF8A90A6);
        small.setTextSize(11 * d);
        small.setLetterSpacing(0.12f);
        small.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(int value, int goal, String label, int colorStart, int colorEnd) {
        this.value = value;
        this.goal = Math.max(1, goal);
        this.label = label;
        this.c1 = colorStart;
        this.c2 = colorEnd;
        updateShader();
        final float target = Math.min(1f, value / (float) this.goal);
        if (anim != null) anim.cancel();
        anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(1300);
        anim.setInterpolator(new DecelerateInterpolator(1.6f));
        anim.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue();
            shown = target * f;
            shownValue = (int) (this.value * f);
            invalidate();
        });
        anim.start();
    }

    private void updateShader() {
        if (getWidth() == 0) return;
        SweepGradient g = new SweepGradient(getWidth() / 2f, getHeight() / 2f,
                new int[]{c1, c2, c1}, null);
        arc.setShader(g);
        glow.setShader(g);
    }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        float pad = 11 * d + 8 * d;
        float r = Math.min(w, h) / 2f - pad;
        rect.set(w / 2f - r, h / 2f - r, w / 2f + r, h / 2f + r);
        updateShader();
    }

    @Override protected void onDraw(Canvas canvas) {
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        canvas.drawOval(rect, track);
        if (shown > 0.001f) {
            canvas.save();
            canvas.rotate(-90, cx, cy);
            canvas.drawArc(rect, 0, 360 * shown, false, glow);
            canvas.drawArc(rect, 0, 360 * shown, false, arc);
            canvas.restore();
        }
        canvas.drawText(label, cx, cy - 26 * d, small);
        canvas.drawText(String.valueOf(shownValue), cx, cy + 10 * d, big);
        canvas.drawText("of " + String.format(java.util.Locale.US, "%,d", goal), cx, cy + 30 * d, small);
    }
}