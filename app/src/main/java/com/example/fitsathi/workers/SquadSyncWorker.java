package com.example.fitsathi.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.fitsathi.data.AppDatabase;
import com.example.fitsathi.managers.SquadManager;
import com.example.fitsathi.managers.UserInfoManager;
import com.example.fitsathi.models.Squad;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SquadSyncWorker extends Worker {

    private static final String TAG = "SquadSyncWorker";

    public SquadSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting squad stats sync...");

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return Result.failure();

        // 1. Standardize to UTC for global competition consistency
        java.util.TimeZone utc = java.util.TimeZone.getTimeZone("UTC");
        
        // Calculate Week Range (Monday to Sunday)
        Calendar cal = Calendar.getInstance(utc);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startTime = cal.getTimeInMillis();
        
        cal.add(Calendar.DAY_OF_WEEK, 6);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        long endTime = cal.getTimeInMillis();

        // Get Step log dates for the week
        List<String> weekDates = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        sdf.setTimeZone(utc);
        
        Calendar weekCal = Calendar.getInstance(utc);
        weekCal.setTimeInMillis(startTime);
        for (int i = 0; i < 7; i++) {
            weekDates.add(sdf.format(weekCal.getTime()));
            weekCal.add(Calendar.DAY_OF_WEEK, 1);
        }

        // Get Weekly Stats from Room
        AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
        int totalSteps = db.stepLogDao().getTotalStepsForDates(weekDates);
        int totalCalories = db.workoutLogDao().getCaloriesForRange(startTime, endTime);

        Log.d(TAG, "Stats: Steps=" + totalSteps + ", Calories=" + totalCalories);

        // 2. Synchronous Wait for Firebase updates
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        final String[] errorMsg = {null};

        // Get User Display Name first
        UserInfoManager.getUserInfo(userInfo -> {
            String name = (userInfo != null && userInfo.getName() != null) ? userInfo.getName() : "User";
            
            // Sync to all user's squads
            String weekId = SquadManager.getCurrentWeekId();
            DatabaseReference baseRef = FirebaseDatabase.getInstance().getReference();

            SquadManager.getUserSquads((squads, error) -> {
                if (squads != null && !squads.isEmpty()) {
                    Map<String, Object> updates = new HashMap<>();
                    for (com.example.fitsathi.models.Squad squad : squads) {
                        String path = "/squad_stats/" + squad.getId() + "/" + weekId + "/" + uid;
                        Map<String, Object> statMap = new HashMap<>();
                        statMap.put("steps", totalSteps);
                        statMap.put("calories", totalCalories);
                        statMap.put("displayName", name);
                        updates.put(path, statMap);
                    }
                    baseRef.updateChildren(updates).addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            errorMsg[0] = task.getException() != null ? task.getException().getMessage() : "Update failed";
                        }
                        latch.countDown();
                    });
                } else {
                    if (error != null) errorMsg[0] = error;
                    latch.countDown();
                }
            });
        });

        try {
            // Wait max 30 seconds for network ops
            if (!latch.await(30, java.util.concurrent.TimeUnit.SECONDS)) {
                return Result.retry();
            }
        } catch (InterruptedException e) {
            return Result.retry();
        }

        if (errorMsg[0] != null) {
            Log.e(TAG, "Sync failed: " + errorMsg[0]);
            return Result.retry();
        }

        return Result.success();
    }
}
