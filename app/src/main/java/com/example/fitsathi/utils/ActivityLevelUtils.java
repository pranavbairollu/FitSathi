package com.example.fitsathi.utils;

import java.util.HashMap;
import java.util.Map;

public class ActivityLevelUtils {

    private static final Map<String, String> activityLevelDescriptions = new HashMap<>();

    static {
        activityLevelDescriptions.put("Sedentary", "Little or no exercise, desk job lifestyle.");
        activityLevelDescriptions.put("Lightly Active", "Light exercise/sports 1–3 days a week.");
        activityLevelDescriptions.put("Moderately Active", "Moderate exercise/sports 3–5 days a week.");
        activityLevelDescriptions.put("Very Active", "Hard exercise/sports 6–7 days a week.");
        activityLevelDescriptions.put("Extra Active", "Very hard exercise, physical job, or training twice a day.");
    }

    public static String getDescription(String activityLevel) {
        if (activityLevelDescriptions.containsKey(activityLevel)) {
            return activityLevelDescriptions.get(activityLevel);
        }
        return "No description available.";
    }
}
