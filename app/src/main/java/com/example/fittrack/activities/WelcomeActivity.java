package com.example.fittrack.activities;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fittrack.R;
import com.example.fittrack.database.DBHelper;
import com.example.fittrack.model.UserProfile;
import com.example.fittrack.utils.Fx;

public class WelcomeActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        UserProfile p = new DBHelper(this).getProfile();
        String name = (p != null) ? Fx.cap(p.getName()) : "there";
        final String full = "Hello, " + name;

        TextView tvHello = findViewById(R.id.tvHello);
        TextView tvSub = findViewById(R.id.tvSub);
        View logo = findViewById(R.id.logo);
        View pulse = findViewById(R.id.pulse);

        // logo pops in
        logo.setScaleX(0.2f);
        logo.setScaleY(0.2f);
        logo.animate().scaleX(1f).scaleY(1f).setDuration(700)
                .setInterpolator(new OvershootInterpolator(2f)).start();

        // soft pulse ring, repeats
        ObjectAnimator pulseAnim = ObjectAnimator.ofPropertyValuesHolder(pulse,
                PropertyValuesHolder.ofFloat("scaleX", 0.6f, 1.25f),
                PropertyValuesHolder.ofFloat("scaleY", 0.6f, 1.25f),
                PropertyValuesHolder.ofFloat("alpha", 0.9f, 0f));
        pulseAnim.setDuration(1400);
        pulseAnim.setRepeatCount(ObjectAnimator.INFINITE);
        pulseAnim.start();

        // typewriter: untyped letters stay transparent so the text does not jump around
        tvHello.setAlpha(1f);
        tvHello.setText(hide(full, 0));
        handler.postDelayed(new Runnable() {
            int i = 0;
            @Override
            public void run() {
                i++;
                tvHello.setText(hide(full, i));
                if (i < full.length()) {
                    handler.postDelayed(this, 80);
                } else {
                    tvSub.setTranslationY(40f);
                    tvSub.animate().alpha(1f).translationY(0f).setDuration(700).start();
                    handler.postDelayed(() -> {
                        startActivity(new Intent(WelcomeActivity.this, DashboardActivity.class));
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        finish();
                    }, 2000);
                }
            }
        }, 900);
    }

    private SpannableString hide(String full, int shown) {
        SpannableString s = new SpannableString(full);
        if (shown < full.length()) {
            s.setSpan(new ForegroundColorSpan(Color.TRANSPARENT), shown, full.length(), 0);
        }
        return s;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
