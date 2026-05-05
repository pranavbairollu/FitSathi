package com.example.fitsathi.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.fitsathi.utils.DateUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class WaterIntakeManager {

    private static final String TAG = "WaterIntakeManager";
    private static final String FIREBASE_WATER_LOG_KEY = "water_logs";
    private static final String PREF_NAME = "WaterPrefs";
    private static final String KEY_GOAL = "water_goal";
    private static final int DEFAULT_GOAL = 8;
    private static final Gson gson = new Gson();

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
        // 1. Load from local cache first
        List<Long> localLog = getLocalLog(context, date);
        callback.onWaterLogReceived(localLog);

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
                    callback.onWaterLogReceived(remoteLog);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Firebase sync cancelled: " + databaseError.getMessage());
            }
        });
    }

    public static void addWaterEntry(Context context, String date, OperationCallback callback) {
        long timestamp = System.currentTimeMillis();
        
        // 1. Update Local
        List<Long> log = getLocalLog(context, date);
        log.add(timestamp);
        saveLocalLog(context, date, log);

        // 2. Update Firebase
        DatabaseReference ref = getWaterLogRef();
        if (ref != null) {
            ref.child(date).push().setValue(timestamp).addOnCompleteListener(task -> {
                if (callback != null) callback.onComplete(task.isSuccessful());
            });
        } else if (callback != null) {
            callback.onComplete(true); // Success locally
        }
    }

    public static void removeWaterEntry(Context context, String date, OperationCallback callback) {
        // 1. Update Local
        List<Long> log = getLocalLog(context, date);
        if (log.isEmpty()) {
            if (callback != null) callback.onComplete(false);
            return;
        }
        log.remove(log.size() - 1);
        saveLocalLog(context, date, log);

        // 2. Update Firebase (Removes the last entry)
        DatabaseReference ref = getWaterLogRef();
        if (ref != null) {
            ref.child(date).limitToLast(1).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult().hasChildren()) {
                    for (DataSnapshot child : task.getResult().getChildren()) {
                        child.getRef().removeValue().addOnCompleteListener(t -> {
                            if (callback != null) callback.onComplete(t.isSuccessful());
                        });
                    }
                } else if (callback != null) {
                    callback.onComplete(false);
                }
            });
        } else if (callback != null) {
            callback.onComplete(true);
        }
    }

    private static List<Long> getLocalLog(Context context, String date) {
        SharedPreferences prefs = SecurePrefsManager.getPrefs(context, PREF_NAME);
        String json = prefs.getString(date + "_log", null);
        if (json != null) {
            Type type = new TypeToken<ArrayList<Long>>() {}.getType();
            return gson.fromJson(json, type);
        }
        return new ArrayList<>();
    }

    private static void saveLocalLog(Context context, String date, List<Long> log) {
        SharedPreferences prefs = SecurePrefsManager.getPrefs(context, PREF_NAME);
        prefs.edit()
                .putInt(date, log.size()) // Compatibility with old logic
                .putString(date + "_log", gson.toJson(log))
                .apply();
    }

    public static int getWaterGoal(Context context) {
        return SecurePrefsManager.getPrefs(context, PREF_NAME)
                .getInt(KEY_GOAL, DEFAULT_GOAL);
    }

    public static void setWaterGoal(Context context, int goal) {
        SecurePrefsManager.getPrefs(context, PREF_NAME)
                .edit()
                .putInt(KEY_GOAL, goal)
                .apply();
    }
}
