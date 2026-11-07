package com.example.fittrack.utils;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

public final class Anim {
    private Anim() {}

    /** Staggered fade-up entrance, in the order given. */
    public static void enter(View... views) {
        for (int i = 0; i < views.length; i++) {
            View v = views[i];
            v.setAlpha(0f);
            v.setTranslationY(32 * v.getResources().getDisplayMetrics().density);
            v.animate().alpha(1f).translationY(0f)
                    .setStartDelay(90L * i).setDuration(550)
                    .setInterpolator(new DecelerateInterpolator(2f)).start();
        }
    }

    /** Button squishes slightly when pressed. */
    @SuppressLint("ClickableViewAccessibility")
    public static void press(View v) {
        v.setOnTouchListener((view, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    view.animate().scaleX(0.96f).scaleY(0.96f).setDuration(90).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.animate().scaleX(1f).scaleY(1f).setDuration(250)
                            .setInterpolator(new OvershootInterpolator(3f)).start();
                    break;
            }
            return false;   // let clicks still work
        });
    }

    /** Card tilts in 3D toward your finger and springs back. */
    @SuppressLint("ClickableViewAccessibility")
    public static void tilt(View v) {
        v.setClickable(true);
        v.setCameraDistance(v.getResources().getDisplayMetrics().density * 9000f);
        v.setOnTouchListener((view, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    view.animate().cancel();
                    float nx = Math.max(-1f, Math.min(1f, (e.getX() / view.getWidth() - 0.5f) * 2f));
                    float ny = Math.max(-1f, Math.min(1f, (e.getY() / view.getHeight() - 0.5f) * 2f));
                    view.setRotationY(nx * 8f);
                    view.setRotationX(-ny * 8f);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.animate().rotationX(0f).rotationY(0f).setDuration(400)
                            .setInterpolator(new OvershootInterpolator(2f)).start();
                    break;
            }
            return false;
        });
    }
}