package com.example.fittrack.utils;

public class CalorieCalculator {

    // Calories = MET x weight(kg) x duration(hours)
    public static double forWorkout(String type, double weightKg, int minutes) {
        double met;
        switch (type) {
            case "Walking": met = 3.5; break;
            case "Running": met = 9.8; break;
            case "Cycling": met = 7.5; break;
            case "Yoga":    met = 2.5; break;
            case "Gym":     met = 6.0; break;
            default:        met = 4.0;
        }
        return met * weightKg * (minutes / 60.0);
    }

    public static double forSteps(int steps, double weightKg) {
        return steps * 0.0005 * weightKg;
    }
}
