package com.example.fitsathi.managers;

import android.content.Context;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import com.example.fitsathi.data.AppDatabase;
import com.example.fitsathi.data.entities.WaterLog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WaterIntakeManager {

    private static final String TAG = "WaterIntakeManager";
    private static final String FIREBASE_WATER_LOG_KEY = "water_logs";
    private static final String PREF_NAME = "WaterPrefs";
    private static final String KEY_GOAL = "water_goal";
    private static final int DEFAULT_GOAL = 8;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface WaterLogCallback {
        void onWaterLogReceived(List<Long> waterLog);
    }

    public interface OperationCallback {
        void onComplete(boolean success);
    }

    private static DatabaseReference getWaterLogRef() {
        try {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() == null) return null;
            String userId = auth.getCurrentUser().getUid();
            return FirebaseDatabase.getInstance().getReference(FIREBASE_WATER_LOG_KEY).child(userId);
        } catch (Exception e) {
            Log.e(TAG, "Firebase not initialized or unavailable: " + e.getMessage());
            return null;
        }
    }

    /**
     * Fetch water log for a specific date. 
     * Prioritizes local cache for speed, then updates from Firebase.
     */
    public static void getWaterLog(Context context, String date, WaterLogCallback callback) {
        // 1. Load from local cache first (Async)
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(context);
            WaterLog log = db.waterLogDao().getWaterLogForDate(date);
            List<Long> localLog = (log != null) ? log.getIntakeList() : new ArrayList<>();
            
            // Notify caller with local data immediately
            if (callback != null) {
                mainHandler.post(() -> callback.onWaterLogReceived(localLog));
            }

            // 2. Sync with Firebase
            DatabaseReference ref = getWaterLogRef();
            if (ref == null) return;

            ref.child(date).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    List<Long> remoteLog = new ArrayList<>();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Long timestamp = snapshot.getValue(Long.class);
                        if (timestamp != null) remoteLog.add(timestamp);
                    }
                    
                    // If remote is different from local, update local and notify
                    if (!remoteLog.equals(localLog)) {
                        saveLocalLog(context, date, remoteLog);
                        if (callback != null) {
                            mainHandler.post(() -> callback.onWaterLogReceived(remoteLog));
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(TAG, "Firebase sync cancelled: " + databaseError.getMessage());
                }
            });
        });
    }

    public static void addWaterEntry(Context context, String date, OperationCallback callback) {
        long timestamp = System.currentTimeMillis();
        
        executor.execute(() -> {
            // 1. Update Local
            AppDatabase db = AppDatabase.getDatabase(context);
            WaterLog logEntity = db.waterLogDao().getWaterLogForDate(date);
            List<Long> log = (logEntity != null) ? logEntity.getIntakeList() : new ArrayList<>();
            log.add(timestamp);
            db.waterLogDao().insertOrUpdate(new WaterLog(date, log));

            // 2. Update Firebase
            DatabaseReference ref = getWaterLogRef();
            if (ref != null) {
                ref.child(date).push().setValue(timestamp).addOnCompleteListener(task -> {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onComplete(task.isSuccessful()));
                    }
                });
            } else if (callback != null) {
                mainHandler.post(() -> callback.onComplete(true));
            }
        });
    }

    public static void removeWaterEntry(Context context, String date, OperationCallback callback) {
        executor.execute(() -> {
            // 1. Update Local
            AppDatabase db = AppDatabase.getDatabase(context);
            WaterLog logEntity = db.waterLogDao().getWaterLogForDate(date);
            if (logEntity == null || logEntity.getIntakeList().isEmpty()) {
                if (callback != null) callback.onComplete(false);
                return;
            }
            List<Long> log = logEntity.getIntakeList();
            log.remove(log.size() - 1);
            db.waterLogDao().insertOrUpdate(new WaterLog(date, log));

            // 2. Update Firebase (Removes the last entry)
            DatabaseReference ref = getWaterLogRef();
            if (ref != null) {
                ref.child(date).limitToLast(1).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().hasChildren()) {
                        for (DataSnapshot child : task.getResult().getChildren()) {
                            child.getRef().removeValue().addOnCompleteListener(t -> {
                                if (callback != null) {
                                    mainHandler.post(() -> callback.onComplete(t.isSuccessful()));
                                }
                            });
                        }
                    } else if (callback != null) {
                        mainHandler.post(() -> callback.onComplete(false));
                    }
                });
            } else if (callback != null) {
                mainHandler.post(() -> callback.onComplete(true));
            }
        });
    }

    private static void saveLocalLog(Context context, String date, List<Long> log) {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(context);
            db.waterLogDao().insertOrUpdate(new WaterLog(date, log));
        });
    }

    public static int getWaterGoal(Context context) {
        // Goal remains in secure prefs for simplicity
        return SecurePrefsManager.getPrefs(context, PREF_NAME)
                .getInt(KEY_GOAL, DEFAULT_GOAL);
    }

    public static void setWaterGoal(Context context, int goal) {
        SecurePrefsManager.getPrefs(context, PREF_NAME)
                .edit()
                .putInt(KEY_GOAL, goal)
                .apply();
    }

    public interface WaterHistoryCallback {
        void onWaterHistoryReceived(Map<String, Integer> waterHistory);
    }

    public static void getLastNDaysWater(Context context, int days, WaterHistoryCallback callback) {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(context);
            
            // Get date range
            java.util.Calendar cal = java.util.Calendar.getInstance();
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            
            Map<String, Integer> history = new java.util.LinkedHashMap<>();
            
            // Prepare dates in reverse order to match getLastNDaysSteps logic if needed, 
            // but usually we want chronological for charts.
            // Let's use chronological.
            java.util.Calendar rangeCal = java.util.Calendar.getInstance();
            rangeCal.add(java.util.Calendar.DAY_OF_YEAR, -(days - 1));
            
            for (int i = 0; i < days; i++) {
                String dateKey = sdf.format(rangeCal.getTime());
                com.example.fitsathi.data.entities.WaterLog log = db.waterLogDao().getWaterLogForDate(dateKey);
                history.put(dateKey, log != null ? log.getIntakeList().size() : 0);
                rangeCal.add(java.util.Calendar.DAY_OF_YEAR, 1);
            }

            if (callback != null) {
                mainHandler.post(() -> callback.onWaterHistoryReceived(history));
            }
        });
    }
}
