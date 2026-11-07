package com.example.fittrack.activities;

import android.animation.ObjectAnimator;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.fittrack.R;
import com.example.fittrack.database.DBHelper;
import com.example.fittrack.model.FoodItem;
import com.example.fittrack.model.UserProfile;
import com.example.fittrack.utils.CalorieCalculator;
import com.example.fittrack.utils.AiClient;
import com.example.fittrack.utils.Fx;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AssistantActivity extends AppCompatActivity {

    private DBHelper db;
    private UserProfile profile;
    private SharedPreferences prefs;

    private LinearLayout chatList, chipList;
    private ScrollView chatScroll;
    private View chipRow;
    private EditText etMsg;

    private View typingView;
    private final List<ObjectAnimator> typingAnims = new ArrayList<>();
    private final List<String[]> history = new ArrayList<>();
    private boolean busy = false;

    private final String[] prompts = {
            "How am I doing today?",
            "Dinner ideas for my remaining calories",
            "Healthy snack under 150 kcal",
            "Tips to reach my step goal"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_assistant);

        // keeps the input above the keyboard
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.assistRoot), (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        db = new DBHelper(this);
        prefs = getSharedPreferences("fittrack", MODE_PRIVATE);
        profile = db.getProfile();
        if (profile == null) { finish(); return; }

        chatList = findViewById(R.id.chatList);
        chatScroll = findViewById(R.id.chatScroll);
        chipList = findViewById(R.id.chipList);
        chipRow = findViewById(R.id.chipRow);
        etMsg = findViewById(R.id.etMsg);

        View btnSend = findViewById(R.id.btnSend);
        findViewById(R.id.btnBackAi).setOnClickListener(v -> finish());
        findViewById(R.id.btnKey).setOnClickListener(v -> askForKey());
        btnSend.setOnClickListener(v -> send(etMsg.getText().toString()));
        Fx.pressable(btnSend);

        buildChips();

        chatList.postDelayed(() -> {
            if (isFinishing()) return;
            addBubble("Hi " + Fx.cap(profile.getName()) + "! I'm your FitTrack coach. "
                    + "I can see your steps and meals for today, so ask me about food, "
                    + "workouts or how your day is going.", false);
            if (apiKey().isEmpty() && !AiClient.USE_LOCAL) {
                addBubble("To start chatting, tap the gear icon at the top right and paste "
                        + "your Gemini API key.", false);
                chatList.postDelayed(() -> { if (!isFinishing()) askForKey(); }, 600);
            }
        }, 350);
    }

    // ---------- chat ----------

    private void send(String raw) {
        String text = raw.trim();
        if (text.isEmpty() || busy) return;

        addBubble(text, true);
        history.add(new String[]{"user", text});
        etMsg.setText("");
        chipRow.setVisibility(View.GONE);
        busy = true;
        showTyping();

        AiClient.chat(apiKey(), buildSystemPrompt(), recentHistory(), new AiClient.Callback() {
            @Override
            public void onReply(String reply) {
                if (isDestroyed()) return;
                hideTyping();
                history.add(new String[]{"assistant", reply});
                addBubble(reply, false);
                busy = false;
            }

            @Override
            public void onError(String message) {
                if (isDestroyed()) return;
                hideTyping();
                history.remove(history.size() - 1);   // drop the unanswered message
                addBubble("Sorry, that didn't work. " + message, false);
                busy = false;
            }
        });
    }

    private List<String[]> recentHistory() {
        int from = Math.max(0, history.size() - 12);
        List<String[]> r = new ArrayList<>(history.subList(from, history.size()));
        while (!r.isEmpty() && !r.get(0)[0].equals("user")) r.remove(0);
        return r;
    }

    private String buildSystemPrompt() {
        String today = DBHelper.today();
        int steps = db.getStepsForDate(today);
        int eaten = db.getFoodCaloriesForDate(today);
        int burned = (int) Math.round(db.getCaloriesForDate(today)
                + CalorieCalculator.forSteps(steps, profile.getWeightKg()));

        StringBuilder meals = new StringBuilder();
        for (FoodItem f : db.getFoodsForDate(today)) {
            meals.append("- ").append(f.getMeal()).append(": ").append(f.getName())
                    .append(" (").append(f.getCalories()).append(" kcal)\n");
        }
        if (meals.length() == 0) meals.append("- nothing logged yet\n");

        String time = new SimpleDateFormat("HH:mm", Locale.US).format(new Date());

        return "You are FitTrack Coach, a friendly and concise fitness and nutrition assistant "
                + "inside a mobile fitness app. Reply in plain text only: no markdown, no asterisks, "
                + "no headings. Keep answers under 120 words, practical and encouraging. "
                + "You are not a doctor: for medical conditions or pain, suggest seeing a professional. "
                + "Never encourage extreme dieting or eating under about 1200 kcal a day.\n\n"
                + "User: " + Fx.cap(profile.getName()) + "\n"
                + "Weight: " + profile.getWeightKg() + " kg\n"
                + "Daily step goal: " + profile.getStepGoal() + "\n"
                + "Daily calorie intake goal: " + profile.getCalorieGoal() + " kcal\n"
                + "Local time: " + time + "\n"
                + "Today so far: " + steps + " steps, " + eaten + " kcal eaten, about "
                + burned + " kcal burned, "
                + Math.max(0, profile.getCalorieGoal() - eaten) + " kcal remaining.\n"
                + "Meals logged today:\n" + meals;
    }

    // ---------- bubbles and animation ----------

    private void addBubble(String text, boolean mine) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(15.5f);
        t.setLineSpacing(0f, 1.12f);
        t.setTextColor(mine ? Color.WHITE : Color.parseColor("#1C1C1E"));
        t.setBackgroundResource(mine ? R.drawable.bg_bubble_user : R.drawable.bg_bubble_ai);
        t.setPadding(dp(16), dp(11), dp(16), dp(11));
        t.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.78f));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = mine ? Gravity.END : Gravity.START;
        lp.topMargin = dp(10);
        chatList.addView(t, lp);

        t.setAlpha(0f);
        t.setTranslationY(dp(24));
        t.setScaleX(0.92f);
        t.setScaleY(0.92f);
        t.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                .setStartDelay(0).setDuration(380)
                .setInterpolator(new OvershootInterpolator(1.1f)).start();
        scrollDown();
    }

    private void showTyping() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setBackgroundResource(R.drawable.bg_bubble_ai);
        box.setPadding(dp(18), dp(14), dp(18), dp(14));

        for (int i = 0; i < 3; i++) {
            TextView d = new TextView(this);
            d.setText("●");
            d.setTextSize(10);
            d.setTextColor(Color.parseColor("#FC4C02"));
            d.setPadding(dp(3), 0, dp(3), 0);
            d.setAlpha(0.25f);
            box.addView(d);

            ObjectAnimator a = ObjectAnimator.ofFloat(d, "alpha", 0.25f, 1f);
            a.setDuration(450);
            a.setStartDelay(i * 180L);
            a.setRepeatCount(ObjectAnimator.INFINITE);
            a.setRepeatMode(ObjectAnimator.REVERSE);
            a.start();
            typingAnims.add(a);
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.START;
        lp.topMargin = dp(10);
        chatList.addView(box, lp);
        typingView = box;
        scrollDown();
    }

    private void hideTyping() {
        for (ObjectAnimator a : typingAnims) a.cancel();
        typingAnims.clear();
        if (typingView != null) {
            chatList.removeView(typingView);
            typingView = null;
        }
    }

    private void scrollDown() {
        chatScroll.postDelayed(() -> chatScroll.smoothScrollTo(0, chatList.getHeight()), 90);
    }

    private void buildChips() {
        for (String p : prompts) {
            TextView chip = new TextView(this);
            chip.setText(p);
            chip.setTextSize(13);
            chip.setTextColor(Color.parseColor("#1C1C1E"));
            chip.setBackgroundResource(R.drawable.bg_chip);
            chip.setPadding(dp(16), dp(10), dp(16), dp(10));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(8));
            chip.setLayoutParams(lp);
            chip.setOnClickListener(v -> send(p));
            Fx.pressable(chip);
            chipList.addView(chip);
        }
    }

    // ---------- API key ----------

    private String apiKey() {
        return prefs.getString("api_key", "");
    }

    private void askForKey() {
        EditText in = new EditText(this);
        in.setHint("AIza...");
        in.setSingleLine(true);
        in.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        LinearLayout box = new LinearLayout(this);
        box.setPadding(dp(24), dp(12), dp(24), 0);
        box.addView(in, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        new AlertDialog.Builder(this)
                .setTitle("Gemini API key")
                .setMessage("Paste a key from aistudio.google.com. It is saved only on this device.")
                .setView(box)
                .setPositiveButton("Save", (d, w) -> {
                    String k = in.getText().toString().trim();
                    if (k.isEmpty()) return;
                    prefs.edit().putString("api_key", k).apply();
                    Toast.makeText(this, "Key saved. Ask away!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (ObjectAnimator a : typingAnims) a.cancel();
    }
}
