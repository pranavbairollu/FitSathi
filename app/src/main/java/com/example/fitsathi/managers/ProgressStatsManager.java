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
        java.util.concurrent.atomic.AtomicBoolean callbackFired = new java.util.concurrent.atomic.AtomicBoolean(false);

        // 1. Load User Info (Firebase)
        try {
            UserInfoManager.getUserInfo(userInfo -> {
                if (userInfo != null) {
                    progress.setUserName(userInfo.getName());
                    progress.setProfilePicUrl(userInfo.getProfilePicUrl());
                }
                safeCheckTasks(pendingTasks, progress, callback, callbackFired);
            });
        } catch (Exception e) {
            safeCheckTasks(pendingTasks, progress, callback, callbackFired);
        }

        // 2. Load Steps History (Firebase)
        try {
            StepCounterManager.getLastNDaysSteps(7, stepsHistory -> {
                progress.setStepsHistory(stepsHistory);
                safeCheckTasks(pendingTasks, progress, callback, callbackFired);
            });
        } catch (Exception e) {
            safeCheckTasks(pendingTasks, progress, callback, callbackFired);
        }

        // 3. Load Water History (Local Room)
        try {
            WaterIntakeManager.getLastNDaysWater(context, 7, waterHistory -> {
                progress.setWaterHistory(waterHistory);
                safeCheckTasks(pendingTasks, progress, callback, callbackFired);
            });
        } catch (Exception e) {
            safeCheckTasks(pendingTasks, progress, callback, callbackFired);
        }

        // 4. Load Calorie History (Firebase)
        try {
            FoodLogManager.getLastNDaysCalories(7, calorieHistory -> {
                progress.setCalorieHistory(calorieHistory);
                safeCheckTasks(pendingTasks, progress, callback, callbackFired);
            });
        } catch (Exception e) {
            safeCheckTasks(pendingTasks, progress, callback, callbackFired);
        }

        // 5. Load Workouts (Room)
        try {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            long endTime = cal.getTimeInMillis();
            cal.add(java.util.Calendar.DAY_OF_YEAR, -7);
            long startTime = cal.getTimeInMillis();

            SimpleDateFormat displaySdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
            progress.setEndDate(displaySdf.format(new Date()));
            progress.setStartDate(displaySdf.format(cal.getTime()));

            new Thread(() -> {
                try {
                    com.example.fitsathi.data.AppDatabase db = com.example.fitsathi.data.AppDatabase.getDatabase(context);
                    java.util.List<com.example.fitsathi.data.entities.WorkoutLog> logs = db.workoutLogDao().getAllHistory();
                    int count = 0;
                    if (logs != null) {
                        for (com.example.fitsathi.data.entities.WorkoutLog log : logs) {
                            if (log.getTimestamp() >= startTime && log.getTimestamp() <= endTime) {
                                count++;
                            }
                        }
                    }
                    progress.setTotalWorkouts(count);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    safeCheckTasks(pendingTasks, progress, callback, callbackFired);
                }
            }).start();
        } catch (Exception e) {
            safeCheckTasks(pendingTasks, progress, callback, callbackFired);
        }

        // Safety Timeout: If data takes longer than 15s, return whatever we have
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (!callbackFired.get()) {
                callbackFired.set(true);
                if (callback != null) callback.onProgressLoaded(progress);
            }
        }, 15000);
    }

    private static void safeCheckTasks(AtomicInteger pendingTasks, WeeklyProgress progress, ProgressCallback callback, java.util.concurrent.atomic.AtomicBoolean callbackFired) {
        if (pendingTasks.decrementAndGet() <= 0) {
            if (!callbackFired.get()) {
                callbackFired.set(true);
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onProgressLoaded(progress));
                }
            }
        }
    }
}
