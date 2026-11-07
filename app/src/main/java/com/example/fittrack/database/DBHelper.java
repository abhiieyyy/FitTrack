package com.example.fittrack.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.fittrack.model.FoodItem;
import com.example.fittrack.model.UserProfile;
import com.example.fittrack.model.Workout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "fittrack.db";
    private static final int DB_VERSION = 2;

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE user(id INTEGER PRIMARY KEY, name TEXT, " +
                "weight_kg REAL, step_goal INTEGER, calorie_goal INTEGER)");
        db.execSQL("CREATE TABLE workout(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "type TEXT, duration_min INTEGER, calories REAL, date TEXT)");
        db.execSQL("CREATE TABLE steps(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "date TEXT UNIQUE, step_count INTEGER)");
        createFoodTable(db);
    }

    private void createFoodTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS food(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT, calories INTEGER, meal TEXT, date TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        // keeps your saved profile; only adds what is new
        if (oldV < 2) createFoodTable(db);
    }

    // ---------- Profile ----------

    public void saveProfile(UserProfile p) {
        ContentValues v = new ContentValues();
        v.put("id", 1);
        v.put("name", p.getName());
        v.put("weight_kg", p.getWeightKg());
        v.put("step_goal", p.getStepGoal());
        v.put("calorie_goal", p.getCalorieGoal());
        getWritableDatabase().insertWithOnConflict("user", null, v,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    /** Returns null if the profile has not been created yet. */
    public UserProfile getProfile() {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT name, weight_kg, step_goal, calorie_goal FROM user WHERE id=1", null);
        UserProfile p = null;
        if (c.moveToFirst()) {
            p = new UserProfile(c.getString(0), c.getDouble(1), c.getInt(2), c.getInt(3));
        }
        c.close();
        return p;
    }

    // ---------- Food ----------

    public long addFood(FoodItem f) {
        ContentValues v = new ContentValues();
        v.put("name", f.getName());
        v.put("calories", f.getCalories());
        v.put("meal", f.getMeal());
        v.put("date", f.getDate());
        return getWritableDatabase().insert("food", null, v);
    }

    public List<FoodItem> getFoodsForDate(String date) {
        List<FoodItem> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id, name, calories, meal, date FROM food WHERE date=? ORDER BY id DESC",
                new String[]{date});
        while (c.moveToNext()) {
            list.add(new FoodItem(c.getInt(0), c.getString(1),
                    c.getInt(2), c.getString(3), c.getString(4)));
        }
        c.close();
        return list;
    }

    public void deleteFood(int id) {
        getWritableDatabase().delete("food", "id=?", new String[]{String.valueOf(id)});
    }

    public int getFoodCaloriesForDate(String date) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT IFNULL(SUM(calories),0) FROM food WHERE date=?", new String[]{date});
        c.moveToFirst();
        int r = c.getInt(0);
        c.close();
        return r;
    }

    // ---------- Workouts ----------

    public long addWorkout(Workout w) {
        ContentValues v = new ContentValues();
        v.put("type", w.getType());
        v.put("duration_min", w.getDuration());
        v.put("calories", w.getCalories());
        v.put("date", w.getDate());
        return getWritableDatabase().insert("workout", null, v);
    }

    public List<Workout> getAllWorkouts() {
        List<Workout> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id, type, duration_min, calories, date FROM workout " +
                "ORDER BY date DESC, id DESC", null);
        while (c.moveToNext()) {
            list.add(new Workout(c.getInt(0), c.getString(1),
                    c.getInt(2), c.getDouble(3), c.getString(4)));
        }
        c.close();
        return list;
    }

    public void deleteWorkout(int id) {
        getWritableDatabase().delete("workout", "id=?", new String[]{String.valueOf(id)});
    }

    public double getCaloriesForDate(String date) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT IFNULL(SUM(calories),0) FROM workout WHERE date=?",
                new String[]{date});
        c.moveToFirst();
        double r = c.getDouble(0);
        c.close();
        return r;
    }

    // ---------- Steps ----------

    public void saveSteps(String date, int count) {
        ContentValues v = new ContentValues();
        v.put("date", date);
        v.put("step_count", count);
        getWritableDatabase().insertWithOnConflict("steps", null, v,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    public int getStepsForDate(String date) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT step_count FROM steps WHERE date=?", new String[]{date});
        int r = c.moveToFirst() ? c.getInt(0) : 0;
        c.close();
        return r;
    }

    public void addSteps(String date, int extra) {
        saveSteps(date, getStepsForDate(date) + extra);
    }

    // ---------- Dates and reports ----------

    public static String dateDaysAgo(int daysAgo) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());
    }

    public static String today() {
        return dateDaysAgo(0);
    }

    public int[] getWeeklySteps() {
        int[] data = new int[7];
        for (int i = 6; i >= 0; i--) data[6 - i] = getStepsForDate(dateDaysAgo(i));
        return data;
    }

    public int[] getWeeklyFoodCalories() {
        int[] data = new int[7];
        for (int i = 6; i >= 0; i--) data[6 - i] = getFoodCaloriesForDate(dateDaysAgo(i));
        return data;
    }
}
