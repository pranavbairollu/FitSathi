package com.example.fitsathi.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.health.connect.client.HealthConnectClient;
import androidx.health.connect.client.records.HydrationRecord;
import androidx.health.connect.client.records.StepsRecord;
import androidx.health.connect.client.records.WeightRecord;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.fitsathi.data.AppDatabase;
import com.example.fitsathi.data.entities.StepLog;
import com.example.fitsathi.data.entities.WaterLog;
import com.example.fitsathi.managers.HealthConnectManager;
import com.example.fitsathi.managers.WeightLogManager;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Background worker to periodically pull data from Health Connect.
 */
public class HealthSyncWorker extends Worker {
    private static final String TAG = "HealthSyncWorker";
    private final HealthConnectManager hcManager;
    private final AppDatabase db;

    public HealthSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.hcManager = HealthConnectManager.getInstance(context);
        this.db = AppDatabase.getDatabase(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting Health Connect background sync...");
        
        if (HealthConnectManager.isHealthConnectAvailable(getApplicationContext()) != HealthConnectClient.SDK_AVAILABLE) {
            return Result.failure();
        }

        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = {true};

        hcManager.checkPermissions(allGranted -> {
            if (!allGranted) {
                Log.w(TAG, "Permissions not granted for Health Connect sync.");
                success[0] = false;
                latch.countDown();
                return;
            }

            syncSteps();
            syncWeight();
            syncHydration();
            
            // For simplicity in this demo, we assume success or handle internal errors
            latch.countDown();
        });

        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Sync interrupted", e);
            return Result.retry();
        }

        return success[0] ? Result.success() : Result.failure();
    }

    private void syncSteps() {
        Instant startTime = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endTime = Instant.now();

        hcManager.readRecords(StepsRecord.class, startTime, endTime, new HealthConnectManager.RecordsCallback<StepsRecord>() {
            @Override
            public void onResult(List<StepsRecord> records) {
                long totalSteps = 0;
                for (StepsRecord record : records) {
                    totalSteps += record.getCount();
                }
                
                String todayKey = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                db.stepLogDao().insertOrUpdate(new StepLog(todayKey, (int) totalSteps));
                Log.d(TAG, "Synced " + totalSteps + " steps from Health Connect");
            }

            @Override
            public void onError(Throwable t) {
                Log.e(TAG, "Error syncing steps", t);
            }
        });
    }

    private void syncWeight() {
        // Sync weight from last 7 days
        Instant startTime = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant endTime = Instant.now();

        hcManager.readRecords(WeightRecord.class, startTime, endTime, new HealthConnectManager.RecordsCallback<WeightRecord>() {
            @Override
            public void onResult(List<WeightRecord> records) {
                for (WeightRecord record : records) {
                    float weightKg = (float) record.getWeight().getKilograms();
                    long timestamp = record.getTime().toEpochMilli();
                    // We don't have a WeightLog Room entity yet, but we can sync to Firebase if needed
                    // For now, let's just log it or update local state if available.
                    // WeightLogManager.saveWeight(getApplicationContext(), weightKg); // This might cause infinite loop if not careful
                }
            }

            @Override
            public void onError(Throwable t) {
                Log.e(TAG, "Error syncing weight", t);
            }
        });
    }

    private void syncHydration() {
        Instant startTime = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endTime = Instant.now();

        hcManager.readRecords(HydrationRecord.class, startTime, endTime, new HealthConnectManager.RecordsCallback<HydrationRecord>() {
            @Override
            public void onResult(List<HydrationRecord> records) {
                List<Long> timestamps = new ArrayList<>();
                for (HydrationRecord record : records) {
                    timestamps.add(record.getStartTime().toEpochMilli());
                }
                
                if (!timestamps.isEmpty()) {
                    String todayKey = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                    db.waterLogDao().insertOrUpdate(new WaterLog(todayKey, timestamps));
                }
            }

            @Override
            public void onError(Throwable t) {
                Log.e(TAG, "Error syncing hydration", t);
            }
        });
    }
}
