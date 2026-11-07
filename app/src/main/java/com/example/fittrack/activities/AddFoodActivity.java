package com.example.fittrack.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
import com.example.fittrack.model.FoodItem;
import com.example.fittrack.model.UserProfile;
import com.example.fittrack.utils.DarkSkin;
import com.example.fittrack.utils.Fx;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddFoodActivity extends AppCompatActivity {

    private final String[] meals = {"Breakfast", "Lunch", "Dinner", "Snack"};
    private final String[] mealIcons = {"\uD83C\uDF73 ", "\uD83C\uDF5B ", "\uD83C\uDF7D ", "\uD83C\uDF4E "};

    private final String[] foodNames = {
            "Banana", "Boiled egg", "Apple", "Roti", "Rice (1 bowl)", "Dal (1 bowl)",
            "Paneer (100g)", "Milk (1 glass)", "Oats (1 bowl)", "Samosa",
            "Paratha", "Idli", "Dosa", "Upma", "Poha", "Chole (1 bowl)", "Rajma (1 bowl)",
            "Curd (1 bowl)", "Chicken curry (1 bowl)", "Fish curry (1 bowl)", "Biryani (1 plate)",
            "Khichdi (1 bowl)", "Sandwich", "Peanuts (30g)", "Almonds (10)", "Orange", "Mango",
            "Chai with sugar", "Coffee with milk", "Biscuits (2)", "Bread slice", "Butter (1 tsp)",
            "Ghee (1 tsp)", "Pizza slice", "Burger", "French fries", "Gulab jamun", "Ice cream (1 scoop)",
            "Cornflakes with milk", "Protein shake"};
    private final int[] foodKcal = {
            105, 78, 95, 120, 200, 180,
            265, 150, 150, 260,
            260, 40, 170, 190, 180, 280, 250,
            100, 300, 220, 450,
            220, 250, 170, 70, 62, 200,
            60, 80, 70, 80, 35,
            45, 285, 540, 365, 150, 140,
            200, 160};

    private final float[] portions = {0.5f, 1f, 1.5f, 2f};
    private final String[] portionLabels = {"\u00BD\u00D7", "1\u00D7", "1.5\u00D7", "2\u00D7"};

    private EditText etName, etCal, etSearch;
    private TextView[] chips, portionChips;
    private TextView tvPreview, btnSave;
    private ProgressBar bar;
    private LinearLayout quickList;
    private int selected = 0;
    private int portionIdx = 1;
    private int baseKcal = 0;
    private boolean programmatic = false;
    private int calGoal = 2000;
    private DBHelper db;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_add_food);

        View root = findViewById(R.id.addFoodRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        // Dark skin + light status-bar icons
        DarkSkin.apply(root);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);

        db = new DBHelper(this);
        prefs = getSharedPreferences("fittrack_food", Context.MODE_PRIVATE);
        UserProfile p = db.getProfile();
        if (p != null) calGoal = p.getCalorieGoal();

        etName = findViewById(R.id.etFoodName);
        etCal = findViewById(R.id.etFoodCal);
        quickList = findViewById(R.id.quickList);
        btnSave = null;
        View saveView = findViewById(R.id.btnSaveFood);
        if (saveView instanceof TextView) btnSave = (TextView) saveView;

        chips = new TextView[]{findViewById(R.id.chipBreakfast), findViewById(R.id.chipLunch),
                findViewById(R.id.chipDinner), findViewById(R.id.chipSnack)};
        for (int i = 0; i < chips.length; i++) {
            final int idx = i;
            chips[i].setText(mealIcons[i] + meals[i]);
            chips[i].setTypeface(Typeface.DEFAULT_BOLD);
            chips[i].setGravity(Gravity.CENTER);
            Fx.pressable(chips[i]);
            chips[i].setOnClickListener(v -> selectMeal(idx));
        }
        selectMeal(defaultMeal());

        buildExtras();
        filterQuickPicks("");

        etCal.addTextChangedListener(new SimpleWatcher(() -> {
            if (!programmatic) {
                int v = parseInt(etCal, 0);
                baseKcal = Math.round(v / portions[portionIdx]);
            }
            refreshPreview();
        }));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        Fx.pressable(saveView);
        saveView.setOnClickListener(v -> save());
    }

    // ---------- NEW: widgets inserted from code ----------

    private void buildExtras() {
        // Search field goes above the quick-pick strip
        etSearch = new EditText(this);
        etSearch.setHint("Search foods\u2026");
        etSearch.setSingleLine(true);
        etSearch.setTextSize(14);
        etSearch.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        etSearch.setHintTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        etSearch.setPadding(dp(16), dp(12), dp(16), dp(12));
        etSearch.setBackground(glass(dp(16), false));
        etSearch.addTextChangedListener(new SimpleWatcher(() ->
                filterQuickPicks(etSearch.getText().toString())));
        addBefore(quickList, etSearch, dp(12));

        // Portion selector, preview and bar go below the calorie field
        View last = etCal;
        TextView portionLabel = smallLabel("PORTION");
        last = addAfter(last, portionLabel, dp(14));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        portionChips = new TextView[portions.length];
        for (int i = 0; i < portions.length; i++) {
            final int idx = i;
            TextView t = new TextView(this);
            t.setText(portionLabels[i]);
            t.setTextSize(13);
            t.setTypeface(Typeface.DEFAULT_BOLD);
            t.setGravity(Gravity.CENTER);
            t.setPadding(dp(6), dp(11), dp(6), dp(11));
            t.setOnClickListener(v -> applyPortion(idx));
            Fx.pressable(t);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(dp(3), 0, dp(3), 0);
            row.addView(t, lp);
            portionChips[i] = t;
        }
        last = addAfter(last, row, dp(8));
        stylePortions();

        tvPreview = new TextView(this);
        tvPreview.setTextColor(ContextCompat.getColor(this, R.color.accent_b));
        tvPreview.setTextSize(13);
        tvPreview.setPadding(dp(16), dp(14), dp(16), dp(14));
        tvPreview.setBackgroundResource(R.drawable.bg_card);
        last = addAfter(last, tvPreview, dp(14));

        bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setVisibility(View.GONE);
        addAfter(last, bar, dp(8));

        refreshPreview();
    }

    /**
     * Calories already logged today. I haven't seen DBHelper, so this returns -1
     * (the "Today" bar stays hidden). Replace the body with your real call, e.g.
     * return db.getTodayCalories();
     */
    private int todayTotal() {
        return -1;
    }

    private void refreshPreview() {
        if (tvPreview == null) return;
        int add = parseInt(etCal, 0);
        int today = todayTotal();
        if (today >= 0) {
            int after = today + add;
            tvPreview.setText(String.format(Locale.US,
                    "Today: %,d \u2192 %,d / %,d kcal", today, after, calGoal));
            bar.setVisibility(View.VISIBLE);
            bar.setProgress(Math.min(100, Math.round(after * 100f / Math.max(1, calGoal))));
        } else {
            tvPreview.setText(add > 0
                    ? String.format(Locale.US, "This entry adds %,d kcal (daily goal %,d)", add, calGoal)
                    : "Pick a food or enter calories to preview");
        }
    }

    // ---------- meals, portions, quick picks ----------

    private int defaultMeal() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (h < 11) return 0;
        if (h < 16) return 1;
        if (h < 19) return 3;
        if (h < 22) return 2;
        return 3;
    }

    private void selectMeal(int idx) {
        selected = idx;
        for (int i = 0; i < chips.length; i++) {
            boolean on = (i == idx);
            chips[i].setBackground(on ? neon(dp(20)) : glass(dp(20), false));
            chips[i].setTextColor(on ? 0xFFFFFFFF
                    : ContextCompat.getColor(this, R.color.text_primary));
        }
    }

    private void applyPortion(int idx) {
        portionIdx = idx;
        stylePortions();
        if (baseKcal > 0) setCal(Math.round(baseKcal * portions[idx]));
        refreshPreview();
    }

    private void stylePortions() {
        for (int i = 0; i < portionChips.length; i++) {
            boolean on = (i == portionIdx);
            portionChips[i].setBackground(on ? neon(dp(20)) : glass(dp(20), false));
            portionChips[i].setTextColor(on ? 0xFFFFFFFF
                    : ContextCompat.getColor(this, R.color.text_primary));
        }
    }

    private void pick(String name, int kcal) {
        etName.setText(name);
        baseKcal = kcal;
        portionIdx = 1;
        stylePortions();
        setCal(kcal);
    }

    private void setCal(int v) {
        programmatic = true;
        etCal.setText(String.valueOf(v));
        programmatic = false;
        refreshPreview();
    }

    private void filterQuickPicks(String query) {
        quickList.removeAllViews();
        String q = query.trim().toLowerCase(Locale.getDefault());
        List<String[]> out = new ArrayList<>();   // {name, kcal}

        if (q.isEmpty()) {
            for (String[] r : loadRecents()) out.add(r);
        }
        for (int i = 0; i < foodNames.length && out.size() < 14; i++) {
            if (q.isEmpty() || foodNames[i].toLowerCase(Locale.getDefault()).contains(q)) {
                boolean dup = false;
                for (String[] r : out) if (r[0].equals(foodNames[i])) { dup = true; break; }
                if (!dup) out.add(new String[]{foodNames[i], String.valueOf(foodKcal[i])});
            }
        }

        if (out.isEmpty()) {
            TextView none = new TextView(this);
            none.setText("No match. Type the name and calories below.");
            none.setTextSize(13);
            none.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            quickList.addView(none);
            return;
        }

        for (String[] item : out) {
            final String name = item[0];
            final int kcal = Integer.parseInt(item[1]);
            TextView t = new TextView(this);
            t.setText(name + "  \u00B7  " + kcal);
            t.setTextSize(13);
            t.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            t.setBackground(glass(dp(20), false));
            t.setPadding(dp(16), dp(10), dp(16), dp(10));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(8));
            t.setLayoutParams(lp);
            Fx.pressable(t);
            t.setOnClickListener(v -> pick(name, kcal));
            quickList.addView(t);
        }
    }

    // ---------- recent foods (SharedPreferences, no DB change) ----------

    private List<String[]> loadRecents() {
        List<String[]> list = new ArrayList<>();
        String raw = prefs.getString("recents", "");
        if (TextUtils.isEmpty(raw)) return list;
        for (String line : raw.split("\n")) {
            int cut = line.lastIndexOf('|');
            if (cut <= 0) continue;
            try {
                Integer.parseInt(line.substring(cut + 1));
                list.add(new String[]{line.substring(0, cut), line.substring(cut + 1)});
            } catch (NumberFormatException ignored) { }
        }
        return list;
    }

    private void rememberRecent(String name, int baseKcalValue) {
        List<String[]> list = loadRecents();
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i)[0].equals(name)) list.remove(i);
        }
        list.add(0, new String[]{name, String.valueOf(baseKcalValue)});
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size() && i < 6; i++) {
            if (i > 0) sb.append('\n');
            sb.append(list.get(i)[0]).append('|').append(list.get(i)[1]);
        }
        prefs.edit().putString("recents", sb.toString()).apply();
    }

    // ---------- save (your validation, plus portion label and success animation) ----------

    private void save() {
        String name = etName.getText().toString().trim();
        String calStr = etCal.getText().toString().trim();

        if (TextUtils.isEmpty(name)) { etName.setError("Enter the food name"); return; }
        if (TextUtils.isEmpty(calStr)) { etCal.setError("Enter the calories"); return; }

        int cal;
        try {
            cal = Integer.parseInt(calStr);
        } catch (NumberFormatException e) {
            etCal.setError("Enter a valid number");
            return;
        }
        if (cal < 1 || cal > 5000) { etCal.setError("Calories must be 1 to 5000"); return; }

        String saved = name;
        if (portionIdx != 1) saved = name + " (" + portionLabels[portionIdx] + ")";

        db.addFood(new FoodItem(saved, cal, meals[selected], DBHelper.today()));
        rememberRecent(name, Math.max(1, Math.round(cal / portions[portionIdx])));

        View sv = findViewById(R.id.btnSaveFood);
        sv.setEnabled(false);
        if (btnSave != null) btnSave.setText("\u2713 Added");
        sv.animate().scaleX(1.08f).scaleY(1.08f).setDuration(160)
                .withEndAction(() -> sv.animate().scaleX(1f).scaleY(1f).setDuration(160).start())
                .start();
        final String toastName = saved;
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Toast.makeText(this, toastName + " added", Toast.LENGTH_SHORT).show();
            finish();
        }, 450);
    }

    // ---------- UI helpers ----------

    private GradientDrawable glass(int radius, boolean unused) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(radius);
        bg.setColor(0x1AFFFFFF);
        bg.setStroke(dp(1), ContextCompat.getColor(this, R.color.surface_stroke));
        return bg;
    }

    private GradientDrawable neon(int radius) {
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{ContextCompat.getColor(this, R.color.accent_a),
                        ContextCompat.getColor(this, R.color.accent_b)});
        bg.setCornerRadius(radius);
        return bg;
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

    /** Finds the view (anchor or one of its ancestors) that sits directly in a vertical LinearLayout. */
    private View verticalSlot(View v) {
        View cur = v;
        while (cur.getParent() instanceof ViewGroup) {
            ViewGroup p = (ViewGroup) cur.getParent();
            if (p instanceof LinearLayout
                    && ((LinearLayout) p).getOrientation() == LinearLayout.VERTICAL) return cur;
            cur = p;
        }
        return null;
    }

    /** Inserts v after anchor and returns v, so calls can be chained. */
    private View addAfter(View anchor, View v, int topMargin) {
        View slot = verticalSlot(anchor);
        if (slot == null) return anchor;
        ViewGroup parent = (ViewGroup) slot.getParent();
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = topMargin;
        parent.addView(v, parent.indexOfChild(slot) + 1, lp);
        return v;
    }

    private void addBefore(View anchor, View v, int bottomMargin) {
        View slot = verticalSlot(anchor);
        if (slot == null) return;
        ViewGroup parent = (ViewGroup) slot.getParent();
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = bottomMargin;
        parent.addView(v, parent.indexOfChild(slot), lp);
    }

    private int parseInt(EditText et, int fallback) {
        try { return Integer.parseInt(et.getText().toString().trim()); }
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
}