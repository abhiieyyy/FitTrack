package com.example.fittrack.activities;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fittrack.R;
import com.example.fittrack.database.DBHelper;
import com.example.fittrack.model.UserProfile;
import com.example.fittrack.utils.CalorieCalculator;
import com.example.fittrack.utils.DarkSkin;
import com.example.fittrack.utils.Fx;

import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private EditText etName, etWeight, etStepGoal, etCalorieGoal;
    private TextView tvAvatar, tvPlan;
    private DBHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profileRoot), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        // NEW: dark high-tech skin
        DarkSkin.apply(findViewById(R.id.profileRoot));
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);

        db = new DBHelper(this);
        etName = findViewById(R.id.etName);
        etWeight = findViewById(R.id.etWeight);
        etStepGoal = findViewById(R.id.etStepGoal);
        etCalorieGoal = findViewById(R.id.etCalorieGoal);
        Button btnSave = findViewById(R.id.btnSave);

        UserProfile p = db.getProfile();
        if (p != null) {
            etName.setText(p.getName());
            etWeight.setText(String.valueOf(p.getWeightKg()));
            etStepGoal.setText(String.valueOf(p.getStepGoal()));
            etCalorieGoal.setText(String.valueOf(p.getCalorieGoal()));
        }

        buildExtras();   // NEW
        Fx.pressable(btnSave);
        btnSave.setOnClickListener(v -> saveProfile());
    }

    // ---------- NEW: extra widgets inserted from code ----------

    private void buildExtras() {
        // Calorie goal presets + plan card go after the calorie field
        View calAnchor = etCalorieGoal;
        tvPlan = new TextView(this);
        tvPlan.setTextColor(ContextCompat.getColor(this, R.color.accent_b));
        tvPlan.setTextSize(13);
        tvPlan.setPadding(dp(16), dp(14), dp(16), dp(14));
        tvPlan.setBackgroundResource(R.drawable.bg_card);
        addAfter(calAnchor, tvPlan, dp(16));

        LinearLayout calRow = row(
                chip("Lose  \u2212500", v -> applyCalorieMode(-500)),
                chip("Maintain", v -> applyCalorieMode(0)),
                chip("Gain  +300", v -> applyCalorieMode(300)));
        addAfter(calAnchor, calRow, dp(10));
        addAfter(calAnchor, smallLabel("QUICK GOALS"), dp(12));

        // Step goal controls go after the step field
        LinearLayout stepRow = row(
                chip("\u22121,000", v -> bump(etStepGoal, -1000)),
                chip("5k", v -> etStepGoal.setText("5000")),
                chip("8k", v -> etStepGoal.setText("8000")),
                chip("10k", v -> etStepGoal.setText("10000")),
                chip("+1,000", v -> bump(etStepGoal, 1000)));
        addAfter(etStepGoal, stepRow, dp(10));

        // Gradient avatar at the very top of the form
        tvAvatar = new TextView(this);
        tvAvatar.setGravity(Gravity.CENTER);
        tvAvatar.setTextColor(0xFFFFFFFF);
        tvAvatar.setTextSize(34);
        tvAvatar.setTypeface(Typeface.DEFAULT_BOLD);
        tvAvatar.setElevation(dp(10));
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{ContextCompat.getColor(this, R.color.accent_a),
                        ContextCompat.getColor(this, R.color.accent_b)});
        g.setShape(GradientDrawable.OVAL);
        tvAvatar.setBackground(g);
        ViewGroup form = (ViewGroup) etName.getParent();
        if (form instanceof LinearLayout) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(88), dp(88));
            lp.gravity = Gravity.CENTER_HORIZONTAL;
            lp.bottomMargin = dp(8);
            form.addView(tvAvatar, 0, lp);
        }

        TextWatcher w = new SimpleWatcher(this::refreshLive);
        etName.addTextChangedListener(w);
        etWeight.addTextChangedListener(w);
        etStepGoal.addTextChangedListener(w);
        refreshLive();

        if (form != null) Fx.staggerIn(form, 80);
    }

    private void refreshLive() {
        String n = etName.getText().toString().trim();
        tvAvatar.setText(n.isEmpty() ? "?" : n.substring(0, 1).toUpperCase(Locale.getDefault()));

        int steps = parseInt(etStepGoal, 0);
        double kg = parseDouble(etWeight, 0);
        if (steps > 0 && kg > 0) {
            double km = steps * 0.00076;
            int kcal = (int) Math.round((double) CalorieCalculator.forSteps(steps, kg));
            tvPlan.setText(String.format(Locale.US,
                    "Your step goal \u2248 %.1f km a day and about %,d kcal burned walking",
                    km, kcal));
        } else {
            tvPlan.setText("Enter your weight and step goal to see your daily plan");
        }
    }

    /** Rough estimate only (about 30 kcal per kg for maintenance), not medical advice. */
    private void applyCalorieMode(int delta) {
        double kg = parseDouble(etWeight, 0);
        if (kg <= 0) {
            Toast.makeText(this, "Enter your weight first", Toast.LENGTH_SHORT).show();
            return;
        }
        int maintain = (int) (Math.round(kg * 30 / 10.0) * 10);
        int target = Math.max(500, Math.min(10000, maintain + delta));
        etCalorieGoal.setText(String.valueOf(target));
    }

    private void bump(EditText et, int delta) {
        int v = Math.max(1000, Math.min(100000, parseInt(et, 8000) + delta));
        et.setText(String.valueOf(v));
    }

    // ---------- small UI helpers ----------

    private TextView chip(String label, View.OnClickListener l) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        t.setTextSize(12);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(6), dp(11), dp(6), dp(11));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(20));
        bg.setColor(0x1AFFFFFF);
        bg.setStroke(dp(1), ContextCompat.getColor(this, R.color.surface_stroke));
        t.setBackground(bg);
        t.setOnClickListener(l);
        Fx.pressable(t);
        return t;
    }

    private LinearLayout row(TextView... chips) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        for (TextView c : chips) {
            LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(dp(3), 0, dp(3), 0);
            r.addView(c, lp);
        }
        return r;
    }

    private TextView smallLabel(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(12);
        t.setLetterSpacing(0.12f);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        return t;
    }

    /** Inserts v directly after anchor inside anchor's parent (vertical LinearLayout). */
    private void addAfter(View anchor, View v, int topMargin) {
        ViewGroup parent = (ViewGroup) anchor.getParent();
        if (!(parent instanceof LinearLayout)) return;
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = topMargin;
        parent.addView(v, parent.indexOfChild(anchor) + 1, lp);
    }

    private int parseInt(EditText et, int fallback) {
        try { return Integer.parseInt(et.getText().toString().trim()); }
        catch (NumberFormatException e) { return fallback; }
    }

    private double parseDouble(EditText et, double fallback) {
        try { return Double.parseDouble(et.getText().toString().trim()); }
        catch (NumberFormatException e) { return fallback; }
    }

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class SimpleWatcher implements TextWatcher {
        private final Runnable r;
        SimpleWatcher(Runnable r) { this.r = r; }
        @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
        @Override public void onTextChanged(CharSequence s, int a, int b, int c) { r.run(); }
        @Override public void afterTextChanged(Editable s) { }
    }

    // ---------- your original save logic (unchanged) ----------

    private void saveProfile() {
        String name = etName.getText().toString().trim();
        String weightStr = etWeight.getText().toString().trim();
        String stepStr = etStepGoal.getText().toString().trim();
        String calStr = etCalorieGoal.getText().toString().trim();

        if (TextUtils.isEmpty(name)) { etName.setError("Please enter your name"); return; }
        if (TextUtils.isEmpty(weightStr)) { etWeight.setError("Please enter your weight"); return; }
        if (TextUtils.isEmpty(stepStr)) { etStepGoal.setError("Please enter a step goal"); return; }
        if (TextUtils.isEmpty(calStr)) { etCalorieGoal.setError("Please enter a calorie goal"); return; }

        double weight;
        int stepGoal, calGoal;
        try {
            weight = Double.parseDouble(weightStr);
            stepGoal = Integer.parseInt(stepStr);
            calGoal = Integer.parseInt(calStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
            return;
        }

        if (weight < 20 || weight > 300) { etWeight.setError("Weight must be 20 to 300 kg"); return; }
        if (stepGoal < 1000 || stepGoal > 100000) { etStepGoal.setError("Goal must be 1,000 to 100,000"); return; }
        if (calGoal < 500 || calGoal > 10000) { etCalorieGoal.setError("Goal must be 500 to 10,000"); return; }

        boolean firstTime = (db.getProfile() == null);
        db.saveProfile(new UserProfile(name, weight, stepGoal, calGoal));

        if (firstTime) {
            startActivity(new Intent(this, WelcomeActivity.class));
            super.finish();
            overridePendingTransition(R.anim.slide_up, R.anim.fade_dim);
            return;
        }

        Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, R.anim.slide_down);
    }
}