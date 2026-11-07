package com.example.fittrack.utils;

import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.fittrack.R;

public final class DarkSkin {
    private DarkSkin() {}

    /** Call once after setContentView, passing the screen's root view. */
    public static void apply(View root) {
        root.setBackgroundResource(R.drawable.bg_screen);
        walk(root);
    }

    /** Also call this on views you inflate later (e.g. food rows). */
    public static void walk(View v) {
        float d = v.getResources().getDisplayMetrics().density;
        if (v instanceof EditText) {
            EditText e = (EditText) v;
            e.setBackgroundResource(R.drawable.bg_input);
            e.setTextColor(ContextCompat.getColor(v.getContext(), R.color.text_primary));
            e.setHintTextColor(ContextCompat.getColor(v.getContext(), R.color.text_secondary));
            e.setPadding((int) (18 * d), (int) (16 * d), (int) (18 * d), (int) (16 * d));
        } else if (v instanceof Button) {
            Button b = (Button) v;
            b.setBackgroundResource(R.drawable.bg_button);
            b.setTextColor(Color.WHITE);
            b.setStateListAnimator(null);
        } else if (v instanceof TextView) {
            TextView t = (TextView) v;
            int c = t.getCurrentTextColor();
            float[] hsv = new float[3];
            Color.colorToHSV(c, hsv);
            boolean colorful = hsv[1] > 0.4f;      // keep orange/accent text
            boolean dark = hsv[2] < 0.55f;
            if (!colorful && dark) {
                boolean faint = hsv[2] > 0.3f;     // greys become secondary text
                t.setTextColor(ContextCompat.getColor(v.getContext(),
                        faint ? R.color.text_secondary : R.color.text_primary));
            }
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) walk(g.getChildAt(i));
        }
    }
}