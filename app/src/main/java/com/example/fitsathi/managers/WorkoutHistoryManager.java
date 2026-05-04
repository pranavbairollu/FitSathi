package com.example.fitsathi.managers;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.fitsathi.models.Exercise;
import com.example.fitsathi.models.WorkoutSession;
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
    private static final String KEY_CURRENT_SESSION = "current_workout_session";

    private SharedPreferences prefs;
    private Gson gson;

    public WorkoutHistoryManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public void saveCurrentSession(WorkoutSession session) {
        if (session == null) {
            prefs.edit().remove(KEY_CURRENT_SESSION).apply();
            return;
        }
        String json = gson.toJson(session);
        prefs.edit().putString(KEY_CURRENT_SESSION, json).apply();
    }

    public WorkoutSession loadCurrentSession() {
        String json = prefs.getString(KEY_CURRENT_SESSION, "");
        if (json.isEmpty()) return null;
        try {
            return gson.fromJson(json, WorkoutSession.class);
        } catch (Exception e) {
            return null;
        }
    }

    public void clearCurrentSession() {
        prefs.edit().remove(KEY_CURRENT_SESSION).apply();
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
     * Save a completed exercise to history with timestamp
     */
    public void addCompletedExercise(Exercise exercise) {
        if (exercise == null) return;
        
        // Set timestamp if not set
        if (exercise.completionTimestamp == 0) {
            exercise.completionTimestamp = System.currentTimeMillis();
        }

        List<Exercise> history = getWorkoutHistory();
        history.add(exercise);
        
        // Keep only last 100 exercises to avoid SharedPreferences size limits
        if (history.size() > 100) {
            history = history.subList(history.size() - 100, history.size());
        }

        String json = gson.toJson(history);
        prefs.edit().putString(KEY_HISTORY, json).apply();
    }

    /**
     * Legacy method for compatibility
     */
    public void addWorkout(Exercise exercise) {
        addCompletedExercise(exercise);
    }

    /**
     * Retrieve workout history as list of Exercise objects
     */
    public List<Exercise> getWorkoutHistory() {
        String json = prefs.getString(KEY_HISTORY, "");
        if (json.isEmpty()) return new ArrayList<>();
        try {
            Type type = new TypeToken<List<Exercise>>() {}.getType();
            List<Exercise> history = gson.fromJson(json, type);
            return history != null ? history : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Clear workout history (optional)
     */
    public void clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply();
    }
}
