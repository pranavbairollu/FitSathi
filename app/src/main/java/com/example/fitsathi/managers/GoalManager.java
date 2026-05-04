package com.example.fitsathi.managers;

import android.content.Context;
import android.content.SharedPreferences;

public class GoalManager {
    private static final String PREF_NAME = "GoalPrefs";
    private static final String KEY_GOAL = "daily_goal";
    private static final int DEFAULT_GOAL = 10000; // default goal if user hasn't set

    // Save custom goal
    public static void setDailyGoal(Context context, int steps) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_GOAL, steps);
        editor.apply();
    }

    // Get saved goal (or default if none set)
    public static int getDailyGoal(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_GOAL, DEFAULT_GOAL);
    }

    // Reset to default
    public static void resetToDefault(Context context) {
        setDailyGoal(context, DEFAULT_GOAL);
    }

    // Check if custom goal is set
    public static boolean isCustomGoalSet(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.contains(KEY_GOAL) && prefs.getInt(KEY_GOAL, DEFAULT_GOAL) != DEFAULT_GOAL;
    }
}
