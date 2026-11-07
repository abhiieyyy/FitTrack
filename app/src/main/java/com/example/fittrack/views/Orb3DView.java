package com.example.fittrack.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class Orb3DView extends View {
    private static final int LAT = 10, LON = 20;
    private final float[][][] base = new float[LAT - 1][LON][3];
    private final float[][][] proj = new float[LAT - 1][LON][3];
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int colA = 0xFF7C5CFF, colB = 0xFF00E0B8;
    private float spin = 0f, dragY = 0f, rotX = 0.45f, lastX, lastY;
    private ValueAnimator animator;
    private final float density = getResources().getDisplayMetrics().density;

    public Orb3DView(Context c, AttributeSet a) {
        super(c, a);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(1f * density);
        for (int i = 0; i < LAT - 1; i++) {
            double lat = Math.PI * (i + 1) / LAT - Math.PI / 2;
            for (int j = 0; j < LON; j++) {
                double lon = 2 * Math.PI * j / LON;
                base[i][j][0] = (float) (Math.cos(lat) * Math.cos(lon));
                base[i][j][1] = (float) Math.sin(lat);
                base[i][j][2] = (float) (Math.cos(lat) * Math.sin(lon));
            }
        }
    }

    public void setColors(int a, int b) { colA = a; colB = b; invalidate(); }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        float r = Math.min(w, h) / 2f;
        glow.setShader(new RadialGradient(w / 2f, h / 2f, r,
                0x447C5CFF, 0x007C5CFF, Shader.TileMode.CLAMP));
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        animator = ValueAnimator.ofFloat(0f, (float) (2 * Math.PI));
        animator.setDuration(22000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> { spin = (float) a.getAnimatedValue(); invalidate(); });
        animator.start();
    }

    @Override protected void onDetachedFromWindow() {
        if (animator != null) animator.cancel();
        super.onDetachedFromWindow();
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = e.getX(); lastY = e.getY();
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            case MotionEvent.ACTION_MOVE:
                dragY += (e.getX() - lastX) * 0.012f;
                rotX = Math.max(-1.2f, Math.min(1.2f, rotX + (e.getY() - lastY) * 0.012f));
                lastX = e.getX(); lastY = e.getY();
                return true;
        }
        return super.onTouchEvent(e);
    }

    private static int lerp(int a, int b, float t) {
        return Color.rgb(
                (int) (Color.red(a) + (Color.red(b) - Color.red(a)) * t),
                (int) (Color.green(a) + (Color.green(b) - Color.green(a)) * t),
                (int) (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t));
    }

    @Override protected void onDraw(Canvas canvas) {
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float R = Math.min(cx, cy) * 0.68f;
        canvas.drawCircle(cx, cy, Math.min(cx, cy), glow);

        float ry = spin + dragY;
        float cY = (float) Math.cos(ry), sY = (float) Math.sin(ry);
        float cX = (float) Math.cos(rotX), sX = (float) Math.sin(rotX);

        for (int i = 0; i < LAT - 1; i++) {
            for (int j = 0; j < LON; j++) {
                float[] b = base[i][j];
                float x1 = b[0] * cY + b[2] * sY;
                float z1 = -b[0] * sY + b[2] * cY;
                float y2 = b[1] * cX - z1 * sX;
                float z2 = b[1] * sX + z1 * cX;
                float s = 3f / (3f - z2);              // perspective
                proj[i][j][0] = cx + x1 * R * s;
                proj[i][j][1] = cy + y2 * R * s;
                proj[i][j][2] = z2;
            }
        }

        for (int i = 0; i < LAT - 1; i++) {
            for (int j = 0; j < LON; j++) {
                float[] p = proj[i][j];
                float[] q = proj[i][(j + 1) % LON];
                drawSeg(canvas, p, q);
                if (i < LAT - 2) drawSeg(canvas, p, proj[i + 1][j]);
            }
        }
        for (int i = 0; i < LAT - 1; i++) {
            for (int j = 0; j < LON; j++) {
                float[] p = proj[i][j];
                float t = (p[2] + 1f) / 2f;
                dot.setColor(lerp(colA, colB, t));
                dot.setAlpha((int) (70 + 185 * t));
                canvas.drawCircle(p[0], p[1], (1.1f + 2.2f * t) * density, dot);
            }
        }
    }

    private void drawSeg(Canvas c, float[] p, float[] q) {
        float t = ((p[2] + q[2]) / 2f + 1f) / 2f;
        line.setColor(lerp(colA, colB, t));
        line.setAlpha((int) (20 + 90 * t));
        c.drawLine(p[0], p[1], q[0], q[1], line);
    }
}