package com.example.fitsathi.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.util.Log;

import com.example.fitsathi.models.Exercise;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class ExerciseLoader {

    private static final String PREFS_NAME = "DailyWorkoutPrefs";
    private static final String PREF_KEY_DATE = "workout_date";
    private static final String PREF_KEY_JSON = "workout_json"; // Using JSON instead of names

    public static List<Exercise> loadExercisesForUserPrefs(Context context, String goal, String level, String location, String timePref) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String todayKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String savedDate = prefs.getString(PREF_KEY_DATE, "");
        String savedJson = prefs.getString(PREF_KEY_JSON, "");

        Gson gson = new Gson();

        // If it's the same day and we have a saved workout, load that
        if (todayKey.equals(savedDate) && !savedJson.isEmpty()) {
            try {
                Type type = new TypeToken<List<Exercise>>() {}.getType();
                List<Exercise> todayList = gson.fromJson(savedJson, type);
                if (todayList != null && !todayList.isEmpty()) {
                    Log.d("ExerciseLoader", "Loaded saved workout for today: " + todayList.size() + " exercises.");
                    return todayList;
                }
            } catch (Exception e) {
                Log.e("ExerciseLoader", "Error parsing saved workout JSON", e);
            }
        }

        // --- Generate a new workout ---
        List<Exercise> allExercises = loadAllExercises(context);
        Log.d("ExerciseLoader", "Generating new workout for today with filters: " + goal + ", " + level + ", " + location + ", " + timePref);
        List<Exercise> filteredList = getFilteredDailyWorkout(allExercises, goal, level, location, timePref);

        // Save the new workout
        String jsonToSave = gson.toJson(filteredList);
        prefs.edit()
                .putString(PREF_KEY_DATE, todayKey)
                .putString(PREF_KEY_JSON, jsonToSave)
                .apply();

        Log.d("ExerciseLoader", "Saved new workout with " + filteredList.size() + " exercises.");
        return filteredList;
    }

    private static List<Exercise> loadAllExercises(Context context) {
        List<Exercise> allExercises = new ArrayList<>();
        InputStream is = null;
        try {
            AssetManager am = context.getAssets();
            is = am.open("exercise.json");
            int size = is.available();
            if (size <= 0) {
                Log.e("ExerciseLoader", "exercise.json is empty");
                return allExercises;
            }
            byte[] buffer = new byte[size];
            is.read(buffer);
            String jsonStr = new String(buffer, "UTF-8");
            JSONArray array = new JSONArray(jsonStr);

            for (int i = 0; i < array.length(); i++) {
                JSONObject exerciseJson = array.getJSONObject(i);
                String baseName = exerciseJson.optString("name", "Unknown Exercise");
                String location = exerciseJson.optString("location", "Home");
                String drawableName = exerciseJson.optString("image", "ic_dummy_exercise");
                String description = exerciseJson.optString("description", "No instructions available.");
                JSONArray musclesArray = exerciseJson.optJSONArray("muscles");
                List<String> muscles = new ArrayList<>();
                if (musclesArray != null) {
                    for (int k = 0; k < musclesArray.length(); k++) {
                        muscles.add(musclesArray.getString(k));
                    }
                }
                double intensity = exerciseJson.optDouble("intensity", 1.0);

                JSONArray variants = exerciseJson.optJSONArray("variants");

                if (variants == null) continue;

                for (int j = 0; j < variants.length(); j++) {
                    JSONObject variantJson = variants.getJSONObject(j);
                    Exercise ex = new Exercise();
                    ex.name = baseName;
                    ex.location = location;
                    ex.description = description;
                    ex.muscles = muscles;
                    ex.intensity = intensity;
                    ex.level = variantJson.optString("level", "Beginner");
                    ex.setsReps = variantJson.optString("reps", "3 sets of 10");
                    ex.duration = variantJson.optInt("duration", 5);
                    String goal = variantJson.optString("goal", "General Fitness");
                    ex.goals = new ArrayList<>();
                    ex.goals.add(goal);
                    
                    int resId = context.getResources().getIdentifier(drawableName, "drawable", context.getPackageName());
                    ex.imageRes = resId != 0 ? resId : android.R.drawable.ic_menu_help;
                    ex.durationSec = ex.duration * 60;
                    ex.calories = (int) (ex.duration * 5 * intensity);
                    allExercises.add(ex);
                }
            }
            Log.d("ExerciseLoader", "Successfully loaded " + allExercises.size() + " total exercise variants.");
        } catch (Exception e) {
            Log.e("ExerciseLoader", "Error loading and parsing exercise.json", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ignored) {}
            }
        }
        return allExercises;
    }

    private static List<Exercise> getFilteredDailyWorkout(List<Exercise> allExercises, String goal, String level, String location, String timePref) {
        if (allExercises == null || allExercises.isEmpty()) return new ArrayList<>();

        // 1. Get the target duration in minutes
        int minDuration;
        int maxDuration;
        switch (timePref != null ? timePref : "Medium") {
            case "Short":
                minDuration = 10;
                maxDuration = 20;
                break;
            case "Long":
                minDuration = 40;
                maxDuration = 60;
                break;
            case "Medium":
            default:
                minDuration = 20;
                maxDuration = 40;
                break;
        }

        // 2. Filter the initial pool of exercises by Goal, Level, and Location
        List<Exercise> pool = new ArrayList<>();
        for (Exercise ex : allExercises) {
            boolean matchesGoal = goal == null || goal.isEmpty() || (ex.goals != null && ex.goals.contains(goal));
            boolean matchesLevel = level == null || level.isEmpty() || (ex.level != null && ex.level.equalsIgnoreCase(level));
            boolean matchesLocation = location == null || location.isEmpty() || (ex.location != null && ex.location.equalsIgnoreCase(location));

            if (matchesGoal && matchesLevel && matchesLocation) {
                pool.add(ex);
            }
        }

        // Fallback: If no matches for level/location, try just goal
        if (pool.isEmpty() && goal != null && !goal.isEmpty()) {
            for (Exercise ex : allExercises) {
                if (ex.goals != null && ex.goals.contains(goal)) {
                    pool.add(ex);
                }
            }
        }

        Log.d("ExerciseLoader", "Found " + pool.size() + " exercises in the initial pool before time filtering.");

        // 3. Shuffle the pool to ensure variety each day
        long seed = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        Collections.shuffle(pool, new Random(seed));

        // 4. Build the final workout list
        List<Exercise> finalWorkout = new ArrayList<>();
        int currentTotalDuration = 0;
        for (Exercise ex : pool) {
            if (currentTotalDuration + ex.duration <= maxDuration) {
                finalWorkout.add(ex);
                currentTotalDuration += ex.duration;
            }
            if (currentTotalDuration >= minDuration) {
                break;
            }
        }

        // 5. Handle empty workout
        if (finalWorkout.isEmpty()) {
            Log.w("ExerciseLoader", "Could not build a workout to fit the time. Returning a fallback card.");
            Exercise fallbackEx = new Exercise();
            fallbackEx.name = "No Matching Exercises";
            fallbackEx.setsReps = "Please adjust your goals in settings.";
            fallbackEx.imageRes = android.R.drawable.ic_menu_help;
            fallbackEx.duration = 5;
            fallbackEx.durationSec = 300;
            finalWorkout.add(fallbackEx);
        }

        return finalWorkout;
    }
}
