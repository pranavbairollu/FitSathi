package com.example.fitsathi.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.fitsathi.DashboardActivity;
import com.example.fitsathi.R;
import com.example.fitsathi.managers.StepCounterManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StepCounterService extends Service implements SensorEventListener {

    public static final String ACTION_STEPS_UPDATED = "steps_updated";

    private SensorManager sensorManager;
    private Sensor stepSensor;
    private SharedPreferences stepPrefs;
    private final IBinder binder = new StepCounterBinder();
    private int currentSteps = 0;


    private static final String CHANNEL_ID = "steps_channel";
    private static final String PREFS_NAME = "StepCounterPrefs";

    // Keys for the new robust step counting logic
    private static final String KEY_DATE = "date"; // Stores the date for the current steps (e.g., "20231120")
    private static final String KEY_DAILY_STEPS = "daily_steps"; // Stores the accumulated steps for KEY_DATE
    private static final String KEY_LAST_SENSOR_VALUE = "last_sensor_value"; // Stores the last raw value from the sensor

    // For periodic saving
    private Handler handler = new Handler();
    private Runnable saveRunnable;
    private static final long SAVE_INTERVAL_MS = 30 * 60 * 1000; // 30 minutes

    public class StepCounterBinder extends Binder {
        public StepCounterService getService() {
            return StepCounterService.this;
        }
    }

    public int getSteps() {
        return currentSteps;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        stepPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        createNotificationChannel();
        Notification loadingNotification = buildNotification(-1); // -1 indicates "Loading..."
        startForeground(loadingNotification);

        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
        } else {
            // If there's no step sensor, we can't do anything.
            stopSelf();
        }

        // Setup periodic saving
        saveRunnable = new Runnable() {
            @Override
            public void run() {
                saveStepsToFirebase();
                handler.postDelayed(this, SAVE_INTERVAL_MS);
            }
        };
        handler.post(saveRunnable); // Start the runnable

        scheduleMidnightReset();
    }

    private void scheduleMidnightReset() {
        android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, MidnightResetReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Schedule for 00:00:01 tomorrow
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.add(java.util.Calendar.DAY_OF_YEAR, 1);
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 1);
        calendar.set(java.util.Calendar.MILLISECOND, 0);

        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, 
                        calendar.getTimeInMillis(), pendingIntent);
            } else {
                alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, 
                        calendar.getTimeInMillis(), pendingIntent);
            }
        }
    }

    private void startForeground(Notification notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH);
        } else {
            startForeground(1, notification);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "RESET_STEPS".equals(intent.getAction())) {
            resetStepsForNewDay();
        }
        // If the service is killed, it will be automatically restarted.
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // Ensure the service restarts if the app is swiped away
        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        restartServiceIntent.setPackage(getPackageName());
        startService(restartServiceIntent);
        super.onTaskRemoved(rootIntent);
    }

    private void resetStepsForNewDay() {
        currentSteps = 0;
        String today = getTodayDateString();
        
        // We need to capture the current sensor value as the new baseline
        // If we don't have a sensor value yet, it will be handled in onSensorChanged
        int lastSensorValue = stepPrefs.getInt(KEY_LAST_SENSOR_VALUE, -1);
        
        SharedPreferences.Editor editor = stepPrefs.edit();
        editor.putString(KEY_DATE, today);
        editor.putInt(KEY_DAILY_STEPS, 0);
        // lastSensorValue remains the same until next sensor event
        editor.apply();
        
        updateNotification(0);
        broadcastSteps(0);
        
        // Reschedule for the next day
        scheduleMidnightReset();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorManager != null && stepSensor != null) {
            sensorManager.unregisterListener(this);
        }
        // Final save and cleanup
        handler.removeCallbacks(saveRunnable);
        saveStepsToFirebase();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_STEP_COUNTER) return;

        int currentSensorValue = (int) event.values[0];
        if (currentSensorValue <= 0) return; // Ignore invalid sensor data

        String today = getTodayDateString();

        // Load saved state
        String lastSavedDate = stepPrefs.getString(KEY_DATE, "");
        int dailySteps = stepPrefs.getInt(KEY_DAILY_STEPS, 0);
        int lastSensorValue = stepPrefs.getInt(KEY_LAST_SENSOR_VALUE, -1);

        // --- ACCURATE MIDNIGHT RESET (Double Check) ---
        if (!today.equals(lastSavedDate)) {
            dailySteps = 0;
            lastSavedDate = today;
        }

        // --- ROBUST REBOOT & SENSOR RESET HANDLING ---
        int stepsThisEvent = 0;
        if (lastSensorValue == -1) {
            // First run: current sensor value is our baseline
            stepsThisEvent = 0;
        } else if (currentSensorValue < lastSensorValue) {
            // Sensor reset (reboot): everything counted since boot is new steps
            stepsThisEvent = currentSensorValue;
        } else {
            // Normal operation
            stepsThisEvent = currentSensorValue - lastSensorValue;
        }

        // Prevent negative steps or massive jumps (e.g. > 10,000 steps in one event)
        if (stepsThisEvent < 0 || stepsThisEvent > 10000) {
            stepsThisEvent = 0;
        }

        int newDailyTotal = dailySteps + stepsThisEvent;
        currentSteps = newDailyTotal;

        // --- PERSIST STATE ---
        SharedPreferences.Editor editor = stepPrefs.edit();
        editor.putString(KEY_DATE, today);
        editor.putInt(KEY_DAILY_STEPS, newDailyTotal);
        editor.putInt(KEY_LAST_SENSOR_VALUE, currentSensorValue);
        editor.apply();

        // --- UPDATE UI & NOTIFICATION ---
        updateNotification(newDailyTotal);
        broadcastSteps(newDailyTotal);
    }

    private void saveStepsToFirebase() {
        int stepsToSave = stepPrefs.getInt(KEY_DAILY_STEPS, 0);
        if (stepsToSave > 0) {
            StepCounterManager.saveSteps(stepsToSave);
        }
    }

    private void broadcastSteps(int steps) {
        Intent intent = new Intent(ACTION_STEPS_UPDATED);
        intent.putExtra("steps", steps);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void updateNotification(int steps) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(1, buildNotification(steps));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private Notification buildNotification(int steps) {
        String contentText = (steps < 0) ? "Initializing tracker..." : 
                           (steps == 0) ? "Starting your journey! 👟" :
                           (steps + " steps taken today. Keep going!");
        
        Intent notificationIntent = new Intent(this, DashboardActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("FitSathi - Active Tracking")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_shoe)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOnlyAlertOnce(true)
                .setNotificationSilent()
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Step Tracking",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows the current step count in a notification.");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private String getTodayDateString() {
        return new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
    }
}
