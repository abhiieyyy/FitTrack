package com.example.fittrack.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.fittrack.utils.DarkSkin;
import com.example.fittrack.R;
import com.example.fittrack.database.DBHelper;
import com.example.fittrack.model.FoodItem;
import com.example.fittrack.model.UserProfile;
import com.example.fittrack.utils.CalorieCalculator;
import com.example.fittrack.utils.Fx;
import com.example.fittrack.views.Orb3DView;
import com.example.fittrack.views.RingView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity implements SensorEventListener {

    private DBHelper db;
    private UserProfile profile;
    private SharedPreferences prefs;

    private TextView tvGreeting, tvName, tvDate, tvAvatar;
    private TextView tvEaten, tvBurned, tvRemaining, tvEmptyFood;
    private RingView ringSteps, ringCal;
    private LinearLayout foodList;

    private SensorManager sensorManager;
    private Sensor stepSensor;

    private int lastSteps = 0, lastEaten = 0, lastBurned = 0, lastRemaining = 0;
    private boolean firstFoodLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_dashboard);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.dashRoot), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        // dark high-tech skin + light status bar icons
        DarkSkin.apply(findViewById(R.id.dashRoot));
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);

        db = new DBHelper(this);
        prefs = getSharedPreferences("fittrack", MODE_PRIVATE);

        tvGreeting = findViewById(R.id.tvGreeting);
        tvName = findViewById(R.id.tvName);
        tvDate = findViewById(R.id.tvDate);
        tvAvatar = findViewById(R.id.tvAvatar);
        tvEaten = findViewById(R.id.tvEaten);
        tvBurned = findViewById(R.id.tvBurned);
        tvRemaining = findViewById(R.id.tvRemaining);
        tvEmptyFood = findViewById(R.id.tvEmptyFood);
        ringSteps = findViewById(R.id.ringSteps);
        ringCal = findViewById(R.id.ringCal);
        foodList = findViewById(R.id.foodList);

        ringSteps.setRingColor(ContextCompat.getColor(this, R.color.accent_a));

        View btnAddFood = findViewById(R.id.btnAddFood);
        View btnAddSteps = findViewById(R.id.btnAddSteps);

        // NEW FIX: dark card look for the white "Add steps" pill
        btnAddSteps.setBackgroundTintList(null);
        btnAddSteps.setBackgroundResource(R.drawable.bg_card);
        if (btnAddSteps instanceof TextView) {
            ((TextView) btnAddSteps).setTextColor(ContextCompat.getColor(this, R.color.accent_b));
        }

        btnAddFood.setOnClickListener(v -> open(AddFoodActivity.class));
        btnAddSteps.setOnClickListener(v -> showAddStepsDialog());
        tvAvatar.setOnClickListener(v -> open(ProfileActivity.class));
        Fx.pressable(btnAddFood);
        Fx.pressable(btnAddSteps);
        Fx.pressable(tvAvatar);

        ViewGroup page = (ViewGroup) ((ViewGroup) findViewById(R.id.dashRoot)).getChildAt(0);

        // 3D orb inserted below the header block (index 1).
        Orb3DView orb = new Orb3DView(this, null);
        page.addView(orb, Math.min(1, page.getChildCount()),
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(200)));

        // entrance animation: every block slides up in turn (the orb joins in)
        View spacer = page.getChildAt(page.getChildCount() - 1);
        spacer.getLayoutParams().height = dp(96);   // room for the floating AI button
        Fx.staggerIn(page, 100);

        buildAssistantButton();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        if (!hasStepPermission()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 100);
        }
    }

    private void buildAssistantButton() {
        TextView fab = new TextView(this);
        fab.setText("\u2726  Ask AI coach");
        fab.setTextColor(Color.WHITE);
        fab.setTextSize(15);
        fab.setTypeface(Typeface.DEFAULT_BOLD);
        fab.setGravity(Gravity.CENTER);
        fab.setBackgroundResource(R.drawable.bg_fab);
        fab.setPadding(dp(22), dp(14), dp(22), dp(14));
        fab.setElevation(dp(8));

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.END);
        lp.setMargins(0, 0, dp(20), dp(36));
        ((ViewGroup) findViewById(android.R.id.content)).addView(fab, lp);

        fab.setScaleX(0f);
        fab.setScaleY(0f);
        fab.animate().scaleX(1f).scaleY(1f).setStartDelay(900).setDuration(500)
                .setInterpolator(new OvershootInterpolator(2.5f)).start();

        fab.setOnClickListener(v -> open(AssistantActivity.class));
        Fx.pressable(fab);
        Fx.bob(fab, dp(6));
    }

    @Override
    protected void onResume() {
        super.onResume();
        profile = db.getProfile();
        if (profile == null) {
            startActivity(new Intent(this, ProfileActivity.class));
            finish();
            return;
        }
        refreshHeader();
        refreshStats();
        refreshFood();
        registerStepSensor();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    // ---------- permission and sensor ----------

    private boolean hasStepPermission() {
        return Build.VERSION.SDK_INT < 29 ||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED;
    }

    private void registerStepSensor() {
        if (stepSensor != null && hasStepPermission()) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        registerStepSensor();
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        // TYPE_STEP_COUNTER = steps since reboot, so only add the difference
        float total = e.values[0];
        float last = prefs.getFloat("last_total", -1f);
        int delta = 0;
        if (last >= 0) delta = (total >= last) ? (int) (total - last) : (int) total;
        prefs.edit().putFloat("last_total", total).apply();
        if (delta > 0 && profile != null) {
            db.addSteps(DBHelper.today(), delta);
            refreshStats();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    // ---------- UI refresh ----------

    private void refreshHeader() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greet = hour < 12 ? "Good morning" : (hour < 17 ? "Good afternoon" : "Good evening");
        tvGreeting.setText(greet);
        tvName.setText(Fx.cap(profile.getName()));
        tvAvatar.setText(Fx.cap(profile.getName()).substring(0, 1));
        tvDate.setText(new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(new Date()));
    }

    private void refreshStats() {
        if (profile == null) return;
        String today = DBHelper.today();
        int steps = db.getStepsForDate(today);
        int eaten = db.getFoodCaloriesForDate(today);
        int burned = (int) Math.round(db.getCaloriesForDate(today)
                + CalorieCalculator.forSteps(steps, profile.getWeightKg()));
        final int stepGoal = profile.getStepGoal();
        final int calGoal = profile.getCalorieGoal();
        int remaining = Math.max(0, calGoal - eaten);

        ringSteps.setProgress(steps / (float) stepGoal);
        // mint when on track, warm orange-red when over the goal
        ringCal.setRingColor(ContextCompat.getColor(this,
                eaten > calGoal ? R.color.accent_warm : R.color.accent_b));
        ringCal.setProgress(eaten / (float) calGoal);

        // numbers count up instead of jumping
        Fx.countUp(ringSteps, lastSteps, steps,
                v -> ringSteps.setCenterText(n(v), "/ " + n(stepGoal)));
        Fx.countUp(tvEaten, lastEaten, eaten, v -> {
            ringCal.setCenterText(n(v), "/ " + n(calGoal) + " kcal");
            tvEaten.setText(n(v));
        });
        Fx.countUp(tvBurned, lastBurned, burned, v -> tvBurned.setText(n(v)));
        Fx.countUp(tvRemaining, lastRemaining, remaining, v -> tvRemaining.setText(n(v)));
        lastSteps = steps;
        lastEaten = eaten;
        lastBurned = burned;
        lastRemaining = remaining;

        if (steps >= stepGoal && !today.equals(prefs.getString("goal_date", ""))) {
            prefs.edit().putString("goal_date", today).apply();
            Toast.makeText(this, "Step goal reached! Great work!", Toast.LENGTH_LONG).show();
        }
    }

    private void refreshFood() {
        foodList.removeAllViews();
        List<FoodItem> foods = db.getFoodsForDate(DBHelper.today());
        tvEmptyFood.setVisibility(foods.isEmpty() ? View.VISIBLE : View.GONE);

        for (FoodItem f : foods) {
            View row = getLayoutInflater().inflate(R.layout.item_food, foodList, false);
            ((TextView) row.findViewById(R.id.tvMealIcon)).setText(f.getMeal().substring(0, 1));
            ((TextView) row.findViewById(R.id.tvFoodName)).setText(f.getName());
            ((TextView) row.findViewById(R.id.tvFoodMeal)).setText(f.getMeal());
            ((TextView) row.findViewById(R.id.tvFoodKcal)).setText(f.getCalories() + " kcal");
            DarkSkin.walk(row);
            Fx.pressable(row);
            row.setOnLongClickListener(v -> {
                new AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert)
                        .setTitle("Delete this entry?")
                        .setMessage(f.getName() + " (" + f.getCalories() + " kcal)")
                        .setPositiveButton("Delete", (d, w) -> {
                            db.deleteFood(f.getId());
                            refreshFood();
                            refreshStats();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return true;
            });
            foodList.addView(row);
        }
        Fx.staggerIn(foodList, firstFoodLoad ? 600 : 0);
        firstFoodLoad = false;
    }

    private void showAddStepsDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("e.g. 1500");
        int pad = dp(24);
        LinearLayout box = new LinearLayout(this);
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        DarkSkin.walk(box);

        new AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert)
                .setTitle("Add steps")
                .setView(box)
                .setPositiveButton("Add", (d, w) -> {
                    String s = input.getText().toString().trim();
                    if (s.isEmpty() || s.length() > 6) {
                        Toast.makeText(this, "Enter a valid number", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int add = Integer.parseInt(s);
                    if (add <= 0 || add > 100000) {
                        Toast.makeText(this, "Enter 1 to 100,000 steps", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    db.addSteps(DBHelper.today(), add);
                    refreshStats();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // opens a screen with the slide-up transition
    private void open(Class<?> target) {
        startActivity(new Intent(this, target));
        overridePendingTransition(R.anim.slide_up, R.anim.fade_dim);
    }

    private String n(int v) {
        return String.format(Locale.US, "%,d", v);
    }

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}