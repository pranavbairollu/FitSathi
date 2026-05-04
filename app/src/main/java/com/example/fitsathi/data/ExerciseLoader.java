package com.example.fitsathi.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.util.Log;

import com.example.fitsathi.models.Exercise;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
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
    private static final String PREF_KEY_NAMES = "workout_names";

    public static List<Exercise> loadExercisesForUserPrefs(Context context, String goal, String level, String location, String timePref) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String todayKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String savedDate = prefs.getString(PREF_KEY_DATE, "");
        String savedNames = prefs.getString(PREF_KEY_NAMES, "");

        List<Exercise> allExercises = loadAllExercises(context);

        // If it's the same day and we have a saved workout, load that
        if (todayKey.equals(savedDate) && !savedNames.isEmpty()) {
            String[] names = savedNames.split(",");
            List<Exercise> todayList = new ArrayList<>();
            for (String name : names) {
                // ✨ FIX: Check that the split operation results in exactly 2 parts
                String[] parts = name.split(" - ");
                if (parts.length == 2) {
                    String exerciseName = parts[0].trim();
                    String exerciseLevel = parts[1].trim();
                    for (Exercise ex : allExercises) {
                        if (ex.name.equals(exerciseName) && ex.level.equals(exerciseLevel)) {
                            todayList.add(ex);
                            break;
                        }
                    }
                } else {
                    // This handles malformed entries, like the old fallback card data
                    Log.w("ExerciseLoader", "Skipping malformed saved exercise: " + name);
                }
            }
            if (!todayList.isEmpty()) {
                Log.d("ExerciseLoader", "Loaded saved workout for today: " + todayList.size() + " exercises.");
                return todayList;
            }
        }

        // --- Generate a new workout ---
        Log.d("ExerciseLoader", "Generating new workout for today with filters: " + goal + ", " + level + ", " + location + ", " + timePref);
        List<Exercise> filteredList = getFilteredDailyWorkout(allExercises, goal, level, location, timePref);

        StringBuilder sb = new StringBuilder();
        for (Exercise ex : filteredList) {
            // ✨ FIX: Only save exercises that are not the fallback and have a valid level
            if (!"No Matching Exercises".equals(ex.name) && ex.level != null && !ex.level.isEmpty()) {
                sb.append(ex.name).append(" - ").append(ex.level).append(",");
            }
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }

        prefs.edit()
                .putString(PREF_KEY_DATE, todayKey)
                .putString(PREF_KEY_NAMES, sb.toString())
                .apply();

        Log.d("ExerciseLoader", "Saved new workout with " + filteredList.size() + " exercises. Names: " + sb.toString());
        return filteredList;
    }

    private static List<Exercise> loadAllExercises(Context context) {
        List<Exercise> allExercises = new ArrayList<>();
        try {
            AssetManager am = context.getAssets();
            InputStream is = am.open("exercise.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String jsonStr = new String(buffer, "UTF-8");
            JSONArray array = new JSONArray(jsonStr);

            for (int i = 0; i < array.length(); i++) {
                JSONObject exerciseJson = array.getJSONObject(i);
                String baseName = exerciseJson.getString("name");
                String location = exerciseJson.getString("location");
                String drawableName = exerciseJson.optString("image", "ic_dummy_exercise");
                JSONArray variants = exerciseJson.getJSONArray("variants");

                for (int j = 0; j < variants.length(); j++) {
                    JSONObject variantJson = variants.getJSONObject(j);
                    Exercise ex = new Exercise();
                    ex.name = baseName;
                    ex.location = location;
                    ex.level = variantJson.getString("level");
                    ex.setsReps = variantJson.getString("reps");
                    ex.duration = variantJson.getInt("duration");
                    String goal = variantJson.getString("goal");
                    ex.goals = new ArrayList<>();
                    ex.goals.add(goal);
                    int resId = context.getResources().getIdentifier(drawableName, "drawable", context.getPackageName());
                    ex.imageRes = resId != 0 ? resId : android.R.drawable.ic_menu_help;
                    ex.durationSec = ex.duration * 60;
                    ex.calories = ex.duration * 5;
                    allExercises.add(ex);
                }
            }
            Log.d("ExerciseLoader", "Successfully loaded " + allExercises.size() + " total exercise variants.");
        } catch (Exception e) {
            Log.e("ExerciseLoader", "Error loading and parsing exercise.json", e);
        }
        return allExercises;
    }

    // ======================================================================
    // ✅ THIS IS THE NEW, CORRECTED FILTERING LOGIC
    // ======================================================================
    private static List<Exercise> getFilteredDailyWorkout(List<Exercise> allExercises, String goal, String level, String location, String timePref) {
        if (allExercises == null || allExercises.isEmpty()) return new ArrayList<>();

        // 1. Get the target duration in minutes
        int minDuration;
        int maxDuration;
        switch (timePref) {
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
            boolean matchesGoal = goal.isEmpty() || (ex.goals != null && ex.goals.contains(goal));
            boolean matchesLevel = level.isEmpty() || (ex.level != null && ex.level.equalsIgnoreCase(level));
            boolean matchesLocation = location.isEmpty() || (ex.location != null && ex.location.equalsIgnoreCase(location));

            if (matchesGoal && matchesLevel && matchesLocation) {
                pool.add(ex);
            }
        }
        Log.d("ExerciseLoader", "Found " + pool.size() + " exercises in the initial pool before time filtering.");

        // 3. Shuffle the pool to ensure variety each day
        // ✅ FIX: Use the day of the year as a seed for the random number generator.
        // This ensures the shuffle is the same for the entire day, but different tomorrow.
        long seed = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        Collections.shuffle(pool, new Random(seed));


        // 4. Build the final workout list by adding exercises until the total time is met
        List<Exercise> finalWorkout = new ArrayList<>();
        int currentTotalDuration = 0;
        for (Exercise ex : pool) {
            // If adding this exercise doesn't exceed the max duration, add it
            if (currentTotalDuration + ex.duration <= maxDuration) {
                finalWorkout.add(ex);
                currentTotalDuration += ex.duration;
            }

            // Stop if we have now reached the minimum required duration
            if (currentTotalDuration >= minDuration) {
                break;
            }
        }
        Log.d("ExerciseLoader", "Final workout has " + finalWorkout.size() + " exercises with a total duration of " + currentTotalDuration + " minutes.");

        // 5. Handle the case where no exercises could be found at all
        if (finalWorkout.isEmpty()) {
            Log.w("ExerciseLoader", "Could not build a workout to fit the time. Returning a fallback card.");
            List<Exercise> fallbackList = new ArrayList<>();
            Exercise fallbackEx = new Exercise();
            fallbackEx.name = "No Matching Exercises";
            fallbackEx.setsReps = "Please adjust your goals in settings.";
            fallbackEx.imageRes = android.R.drawable.ic_menu_help;
            fallbackList.add(fallbackEx);
            return fallbackList;
        }

        return finalWorkout;
    }
}
