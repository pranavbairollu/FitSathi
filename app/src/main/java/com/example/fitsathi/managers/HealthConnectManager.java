package com.example.fitsathi.managers;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.health.connect.client.HealthConnectClient;
import androidx.health.connect.client.PermissionController;
import androidx.health.connect.client.permission.HealthPermission;
import androidx.health.connect.client.records.ExerciseSessionRecord;
import androidx.health.connect.client.records.HydrationRecord;
import androidx.health.connect.client.records.Record;
import androidx.health.connect.client.records.StepsRecord;
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord;
import androidx.health.connect.client.records.WeightRecord;
import androidx.health.connect.client.request.ReadRecordsRequest;
import androidx.health.connect.client.time.TimeRangeFilter;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Singleton manager for all Health Connect operations.
 */
public class HealthConnectManager {
    private static final String TAG = "HealthConnectManager";
    private static HealthConnectManager instance;
    private final HealthConnectClient healthConnectClient;
    private final Context appContext;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static final Set<String> PERMISSIONS = new HashSet<String>(Arrays.asList(
            HealthConnectBridge.getReadPermission(StepsRecord.class),
            HealthConnectBridge.getWritePermission(StepsRecord.class),
            HealthConnectBridge.getReadPermission(WeightRecord.class),
            HealthConnectBridge.getWritePermission(WeightRecord.class),
            HealthConnectBridge.getReadPermission(HydrationRecord.class),
            HealthConnectBridge.getWritePermission(HydrationRecord.class),
            HealthConnectBridge.getReadPermission(ExerciseSessionRecord.class),
            HealthConnectBridge.getWritePermission(ExerciseSessionRecord.class),
            HealthConnectBridge.getReadPermission(TotalCaloriesBurnedRecord.class),
            HealthConnectBridge.getWritePermission(TotalCaloriesBurnedRecord.class)
    ));

    private HealthConnectManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.healthConnectClient = HealthConnectClient.getOrCreate(appContext);
    }

    public static synchronized HealthConnectManager getInstance(Context context) {
        if (instance == null) {
            instance = new HealthConnectManager(context);
        }
        return instance;
    }

    /**
     * Checks if Health Connect is available on the device.
     */
    public static int isHealthConnectAvailable(Context context) {
        return HealthConnectClient.getSdkStatus(context);
    }

    /**
     * Opens the Health Connect app or the Play Store page if not installed.
     */
    public static void installHealthConnect(Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("market://details?id=com.google.android.apps.healthdata&hl=en"));
        intent.setPackage("com.android.vending");
        context.startActivity(intent);
    }

    /**
     * Checks if all required permissions are granted.
     */
    public void checkPermissions(PermissionCallback callback) {
        if (isHealthConnectAvailable(appContext) != HealthConnectClient.SDK_AVAILABLE) {
            callback.onResult(false);
            return;
        }
        executor.execute(() -> {
            try {
                Set<String> grantedPermissions = HealthConnectBridge.getGrantedPermissions(healthConnectClient).get();
                callback.onResult(grantedPermissions.containsAll(PERMISSIONS));
            } catch (Exception e) {
                Log.e(TAG, "Error checking permissions", e);
                callback.onResult(false);
            }
        });
    }

    /**
     * Writes records to Health Connect.
     */
    public void writeRecords(List<? extends Record> records) {
        if (records == null || records.isEmpty()) return;
        
        if (isHealthConnectAvailable(appContext) != HealthConnectClient.SDK_AVAILABLE) {
            Log.e(TAG, "Health Connect SDK not available for writing.");
            return;
        }

        executor.execute(() -> {
            try {
                HealthConnectBridge.insertRecords(healthConnectClient, (List<Record>) records).get();
                Log.d(TAG, "Successfully wrote " + records.size() + " records to Health Connect");
            } catch (Exception e) {
                Log.e(TAG, "Error writing records to Health Connect", e);
                // In a production app, we might want to queue these for retry
            }
        });
    }

    /**
     * Reads records of a specific type within a time range.
     */
    public <T extends Record> void readRecords(Class<T> recordType, Instant startTime, Instant endTime, RecordsCallback<T> callback) {
        if (isHealthConnectAvailable(appContext) != HealthConnectClient.SDK_AVAILABLE) {
            callback.onError(new IllegalStateException("Health Connect SDK not available"));
            return;
        }

        executor.execute(() -> {
            try {
                ReadRecordsRequest<T> request = HealthConnectBridge.createReadRequest(recordType, startTime, endTime);
                List<T> result = HealthConnectBridge.readRecords(healthConnectClient, request).get().getRecords();
                callback.onResult(result);
            } catch (Exception e) {
                Log.e(TAG, "Error reading records from Health Connect", e);
                callback.onError(e);
            }
        });
    }

    public interface PermissionCallback {
        void onResult(boolean allGranted);
    }

    public interface RecordsCallback<T extends Record> {
        void onResult(List<T> records);
        void onError(Throwable t);
    }

    public HealthConnectClient getClient() {
        return healthConnectClient;
    }
}
