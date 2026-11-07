package com.example.fittrack.model;

public class Workout {
    private int id;
    private String type;
    private int duration;      // minutes
    private double calories;
    private String date;       // yyyy-MM-dd

    public Workout(int id, String type, int duration, double calories, String date) {
        this.id = id;
        this.type = type;
        this.duration = duration;
        this.calories = calories;
        this.date = date;
    }

    public Workout(String type, int duration, double calories, String date) {
        this(0, type, duration, calories, date);
    }

    public int getId() { return id; }
    public String getType() { return type; }
    public int getDuration() { return duration; }
    public double getCalories() { return calories; }
    public String getDate() { return date; }
}
