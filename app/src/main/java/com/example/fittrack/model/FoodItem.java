package com.example.fittrack.model;

public class FoodItem {
    private int id;
    private String name;
    private int calories;
    private String meal;   // Breakfast, Lunch, Dinner, Snack
    private String date;   // yyyy-MM-dd

    public FoodItem(int id, String name, int calories, String meal, String date) {
        this.id = id;
        this.name = name;
        this.calories = calories;
        this.meal = meal;
        this.date = date;
    }

    public FoodItem(String name, int calories, String meal, String date) {
        this(0, name, calories, meal, date);
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public int getCalories() { return calories; }
    public String getMeal() { return meal; }
    public String getDate() { return date; }
}
