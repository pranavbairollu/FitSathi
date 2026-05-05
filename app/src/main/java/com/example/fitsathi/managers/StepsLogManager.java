package com.example.fitsathi.managers;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Utility class for saving and retrieving step counts.
 * Stores steps per day in SharedPreferences.
 */
public class StepsLogManager {
    private static final String PREF_NAME = "StepsLogPrefs";
    private static final String DATE_FORMAT = "yyyyMMdd"; // key format (e.g. 20250828)

    /** Save steps for a given date */
    public static void saveStepsForDate(Context context, String dateKey, int steps) {
        SharedPreferences prefs = SecurePrefsManager.getPrefs(context, PREF_NAME);
        prefs.edit().putInt(dateKey, steps).apply();
    }

    /** Save steps for today (overwrites if already exists) */
    public static void saveTodaySteps(Context context, int steps) {
        String todayKey = getTodayKey();
        saveStepsForDate(context, todayKey, steps);
    }

    /** Get steps for a given date */
    public static int getStepsForDate(Context context, String dateKey) {
        SharedPreferences prefs = SecurePrefsManager.getPrefs(context, PREF_NAME);
        return prefs.getInt(dateKey, 0);
    }

    /** Get last 7 days of steps (aligned with labels) */
    public static List<Integer> getLast7DaysSteps(Context context) {
        List<Integer> stepsList = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -6); // 6 days ago
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());

        for (int i = 0; i < 7; i++) {
            String key = sdf.format(cal.getTime());
            int steps = getStepsForDate(context, key);
            stepsList.add(steps);
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        return stepsList;
    }

    /** Helper: returns today's key (yyyyMMdd) */
    private static String getTodayKey() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        return sdf.format(cal.getTime());
    }
}
