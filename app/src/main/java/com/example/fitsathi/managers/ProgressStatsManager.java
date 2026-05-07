package com.example.fitsathi.managers;

import android.content.Context;
import com.example.fitsathi.models.WeeklyProgress;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class ProgressStatsManager {

    public interface ProgressCallback {
        void onProgressLoaded(WeeklyProgress progress);
    }

    public static void getWeeklyProgress(Context context, ProgressCallback callback) {
        WeeklyProgress progress = new WeeklyProgress();
        AtomicInteger pendingTasks = new AtomicInteger(5);

        // 1. Load User Info
        UserInfoManager.getUserInfo(userInfo -> {
            if (userInfo != null) {
                progress.setUserName(userInfo.getName());
                progress.setProfilePicUrl(userInfo.getProfilePicUrl());
            }
            checkTasks(pendingTasks, progress, callback);
        });

        // 2. Load Steps History
        StepCounterManager.getLastNDaysSteps(7, stepsHistory -> {
            progress.setStepsHistory(stepsHistory);
            checkTasks(pendingTasks, progress, callback);
        });

        // 3. Load Water History
        WaterIntakeManager.getLastNDaysWater(context, 7, waterHistory -> {
            progress.setWaterHistory(waterHistory);
            checkTasks(pendingTasks, progress, callback);
        });

        // 4. Load Calorie History
        FoodLogManager.getLastNDaysCalories(7, calorieHistory -> {
            progress.setCalorieHistory(calorieHistory);
            checkTasks(pendingTasks, progress, callback);
        });

        // 5. Load Workouts (Room)
        java.util.Calendar cal = java.util.Calendar.getInstance();
        long endTime = cal.getTimeInMillis();
        cal.add(java.util.Calendar.DAY_OF_YEAR, -7);
        long startTime = cal.getTimeInMillis();

        // Format dates for display
        SimpleDateFormat displaySdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
        progress.setEndDate(displaySdf.format(new Date()));
        progress.setStartDate(displaySdf.format(cal.getTime()));

        new Thread(() -> {
            com.example.fitsathi.data.AppDatabase db = com.example.fitsathi.data.AppDatabase.getDatabase(context);
            // We need a way to count workouts in range. 
            // Let's just get the list and count them for now or add a DAO method.
            // For now, let's use the existing getCaloriesForRange logic but for count.
            // Wait, I can just use getRecentHistory and filter.
            java.util.List<com.example.fitsathi.data.entities.WorkoutLog> logs = db.workoutLogDao().getAllHistory();
            int count = 0;
            for (com.example.fitsathi.data.entities.WorkoutLog log : logs) {
                if (log.getTimestamp() >= startTime && log.getTimestamp() <= endTime) {
                    count++;
                }
            }
            progress.setTotalWorkouts(count);
            checkTasks(pendingTasks, progress, callback);
        }).start();
    }

    private static void checkTasks(AtomicInteger pendingTasks, WeeklyProgress progress, ProgressCallback callback) {
        if (pendingTasks.decrementAndGet() == 0) {
            if (callback != null) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onProgressLoaded(progress));
            }
        }
    }
}
