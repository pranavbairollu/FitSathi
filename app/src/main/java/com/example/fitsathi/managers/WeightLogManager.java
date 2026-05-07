package com.example.fitsathi.managers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.fitsathi.models.WeightLog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class WeightLogManager {

    private static final String TAG = "WeightLogManager";
    private static final String FIREBASE_WEIGHT_LOG_KEY = "weight_logs";

    public interface WeightLogCallback {
        void onLogsReceived(List<WeightLog> weightLogs);
        void onError(String message);
    }

    private static DatabaseReference getDbRef() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            return FirebaseDatabase.getInstance().getReference(FIREBASE_WEIGHT_LOG_KEY)
                    .child(currentUser.getUid());
        }
        return null;
    }

    public static void saveWeight(Context context, float weight) {
        DatabaseReference dbRef = getDbRef();
        long timestamp = System.currentTimeMillis();
        if (dbRef != null) {
            WeightLog log = new WeightLog(timestamp, weight);
            dbRef.push().setValue(log)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Weight saved successfully");
                        // Sync with Health Connect
                        HealthConnectManager hcManager = HealthConnectManager.getInstance(context);
                        hcManager.checkPermissions(allGranted -> {
                            if (allGranted) {
                                hcManager.writeRecords(java.util.Collections.singletonList(
                                        com.example.fitsathi.utils.HealthDataMapper.mapToWeightRecord(log)
                                ));
                            }
                        });
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to save weight", e));
        } else {
            Log.e(TAG, "Cannot save weight, user not authenticated");
        }
    }

    public static void getLastNWeightEntries(Context context, int days, WeightLogCallback callback) {
        DatabaseReference dbRef = getDbRef();
        if (dbRef == null) {
            callback.onError("User not authenticated");
            return;
        }

        long N_DAYS_AGO = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L);
        Query query = dbRef.orderByChild("timestamp").startAt(N_DAYS_AGO);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<WeightLog> weightLogs = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    WeightLog log = snapshot.getValue(WeightLog.class);
                    if (log != null) {
                        weightLogs.add(log);
                    }
                }
                callback.onLogsReceived(weightLogs);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error loading weight logs", databaseError.toException());
                callback.onError(databaseError.getMessage());
            }
        });
    }
}
