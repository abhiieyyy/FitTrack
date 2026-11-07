package com.example.fittrack.model;

public class UserProfile {
    private String name;
    private double weightKg;
    private int stepGoal;
    private int calorieGoal;

    public UserProfile(String name, double weightKg, int stepGoal, int calorieGoal) {
        this.name = name;
        this.weightKg = weightKg;
        this.stepGoal = stepGoal;
        this.calorieGoal = calorieGoal;
    }

    public String getName() { return name; }
    public double getWeightKg() { return weightKg; }
    public int getStepGoal() { return stepGoal; }
    public int getCalorieGoal() { return calorieGoal; }
}
