package com.example.fitsathi.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import com.example.fitsathi.data.AppDatabase;
import com.example.fitsathi.data.entities.WorkoutLog;
import com.example.fitsathi.models.Exercise;
import com.example.fitsathi.models.WorkoutSession;
import com.google.gson.Gson;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkoutHistoryManager {

    private static final String PREF_NAME = "WorkoutHistoryPrefs";
    private static final String KEY_LAST_DATE = "lastWorkoutDate";
    private static final String KEY_DAY_INDEX = "dayIndex";
    private static final String KEY_MODE = "workout_mode";
    private static final String KEY_CURRENT_SESSION = "current_workout_session";

    private SharedPreferences prefs;
    private Gson gson;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface HistoryCallback {
        void onHistoryReceived(List<Exercise> history);
    }

    public WorkoutHistoryManager(Context context) {
        prefs = SecurePrefsManager.getPrefs(context, PREF_NAME);
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

    public void addCompletedExercise(Context context, Exercise exercise) {
        if (exercise == null) return;
        
        if (exercise.completionTimestamp == 0) {
            exercise.completionTimestamp = System.currentTimeMillis();
        }

        executor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(context);
            db.workoutLogDao().insert(new WorkoutLog(
                    exercise.name,
                    exercise.calories,
                    exercise.completionTimestamp,
                    exercise.getDurationCategory()
            ));
        });
    }

    public void getWorkoutHistory(Context context, HistoryCallback callback) {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(context);
            List<WorkoutLog> logs = db.workoutLogDao().getAllHistory();
            List<Exercise> history = new ArrayList<>();
            
            for (WorkoutLog log : logs) {
                Exercise ex = new Exercise();
                ex.name = log.getExerciseName();
                ex.calories = log.getCaloriesBurned();
                ex.completionTimestamp = log.getTimestamp();
                history.add(ex);
            }

            if (callback != null) {
                mainHandler.post(() -> callback.onHistoryReceived(history));
            }
        });
    }

    public void clearHistory(Context context) {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(context);
            db.workoutLogDao().deleteAll();
        });
    }
}
