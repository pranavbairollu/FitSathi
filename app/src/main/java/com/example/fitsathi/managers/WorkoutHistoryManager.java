package com.example.fitsathi.managers;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.fitsathi.models.Exercise;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WorkoutHistoryManager {

    private static final String PREF_NAME = "WorkoutHistoryPrefs";
    private static final String KEY_LAST_DATE = "lastWorkoutDate";
    private static final String KEY_DAY_INDEX = "dayIndex";
    private static final String KEY_MODE = "workout_mode"; // auto or catchup
    private static final String KEY_HISTORY = "workout_history_json";

    private SharedPreferences prefs;
    private Gson gson;

    public WorkoutHistoryManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void setMode(String mode) {
        prefs.edit().putString(KEY_MODE, mode).apply();
    }

    public String getMode() {
        return prefs.getString(KEY_MODE, "auto");
    }

    public int getCurrentDayIndex() {
        String lastDate = prefs.getString(KEY_LAST_DATE, "");
        int index = prefs.getInt(KEY_DAY_INDEX, 0);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        if (getMode().equals("auto") && !today.equals(lastDate)) {
            index = (index + 1) % 7;
            prefs.edit().putInt(KEY_DAY_INDEX, index).apply();
        }
        return index;
    }

    public void markWorkoutCompleted() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        int index = prefs.getInt(KEY_DAY_INDEX, 0);
        prefs.edit().putString(KEY_LAST_DATE, today).putInt(KEY_DAY_INDEX, index).apply();

        if (getMode().equals("catchup")) {
            index = (index + 1) % 7;
            prefs.edit().putInt(KEY_DAY_INDEX, index).apply();
        }
    }

    /**
     * Save a completed workout for today
     */
    public void addWorkout(Exercise exercise) {
        List<Exercise> history = getWorkoutHistory();
        history.add(exercise);
        String json = gson.toJson(history);
        prefs.edit().putString(KEY_HISTORY, json).apply();
    }

    /**
     * Retrieve workout history as list of Exercise objects
     */
    public List<Exercise> getWorkoutHistory() {
        String json = prefs.getString(KEY_HISTORY, "");
        if (json.isEmpty()) return new ArrayList<>();
        Type type = new TypeToken<List<Exercise>>() {}.getType();
        return gson.fromJson(json, type);
    }

    /**
     * Clear workout history (optional)
     */
    public void clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply();
    }
}
