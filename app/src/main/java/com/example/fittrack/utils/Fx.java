package com.example.fittrack.utils;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import java.util.Locale;

/** Small animation toolbox used across the app. */
public class Fx {

    public interface IntSink { void put(int value); }

    public static String cap(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase(Locale.getDefault()) + s.substring(1);
    }

    /** Children fade and slide up one after another. */
    public static void staggerIn(ViewGroup parent, long baseDelay) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View v = parent.getChildAt(i);
            v.setAlpha(0f);
            v.setTranslationY(70f);
            v.animate().alpha(1f).translationY(0f)
                    .setStartDelay(baseDelay + i * 100L)
                    .setDuration(550)
                    .setInterpolator(new DecelerateInterpolator(1.6f))
                    .start();
        }
    }

    /** Squish on press, bounce back on release. Does not block clicks. */
    public static void pressable(View v) {
        v.setOnTouchListener((view, e) -> {
            int a = e.getAction();
            if (a == MotionEvent.ACTION_DOWN) {
                view.animate().scaleX(0.94f).scaleY(0.94f).setStartDelay(0)
                        .setDuration(90).setInterpolator(new DecelerateInterpolator()).start();
            } else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
                view.animate().scaleX(1f).scaleY(1f).setStartDelay(0)
                        .setDuration(220).setInterpolator(new OvershootInterpolator(3f)).start();
            }
            return false;
        });
    }

    /** Animates a number from one value to another. */
    public static void countUp(View key, int from, int to, IntSink sink) {
        Object old = key.getTag();
        if (old instanceof ValueAnimator) ((ValueAnimator) old).cancel();
        if (from == to) { sink.put(to); return; }
        ValueAnimator a = ValueAnimator.ofInt(from, to);
        a.setDuration(850);
        a.setInterpolator(new DecelerateInterpolator());
        a.addUpdateListener(x -> sink.put((Integer) x.getAnimatedValue()));
        key.setTag(a);
        a.start();
    }

    /** Gentle up and down floating. */
    public static void bob(View v, float distancePx) {
        ObjectAnimator a = ObjectAnimator.ofFloat(v, "translationY", 0f, -distancePx);
        a.setDuration(1300);
        a.setRepeatCount(ObjectAnimator.INFINITE);
        a.setRepeatMode(ObjectAnimator.REVERSE);
        a.setInterpolator(new AccelerateDecelerateInterpolator());
        a.start();
    }
}
