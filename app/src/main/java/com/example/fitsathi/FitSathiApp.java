package com.example.fitsathi;

import android.app.Application;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class FitSathiApp extends Application {

    private final java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();

    @Override
    public void onCreate() {
        super.onCreate();
        // Enable Firebase disk persistence for offline support
        try {
            com.google.firebase.database.FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        } catch (Exception e) {
            // Persistence must be set before any other usage of the database
            e.printStackTrace();
        }
        applySavedTheme();
        createNotificationChannels();
        migratePreferences();
        migrateToRoom();
    }

    private void migratePreferences() {
        String[] prefFiles = getResources().getStringArray(R.array.preference_files);
        for (String fileName : prefFiles) {
            com.example.fitsathi.managers.SecurePrefsManager.migrate(this, fileName);
        }
    }

    private void applySavedTheme() {
        android.content.SharedPreferences prefs = com.example.fitsathi.managers.SecurePrefsManager.getPrefs(
                this,
                getString(R.string.settings_prefs_name)
        );
        boolean darkMode = prefs.getBoolean(getString(R.string.dark_mode_enabled_key), false);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                darkMode ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    private void migrateToRoom() {
        android.content.SharedPreferences migrationPrefs = getSharedPreferences("MigrationPrefs", MODE_PRIVATE);
        if (migrationPrefs.getBoolean("room_migrated", false)) return;

        executor.execute(() -> {
            com.example.fitsathi.data.AppDatabase db = com.example.fitsathi.data.AppDatabase.getDatabase(this);
            
            // 1. Migrate Steps
            android.content.SharedPreferences stepsPrefs = com.example.fitsathi.managers.SecurePrefsManager.getPrefs(this, "StepsLogPrefs");
            java.util.Map<String, ?> allSteps = stepsPrefs.getAll();
            for (java.util.Map.Entry<String, ?> entry : allSteps.entrySet()) {
                if (entry.getValue() instanceof Integer) {
                    db.stepLogDao().insertOrUpdate(new com.example.fitsathi.data.entities.StepLog(entry.getKey(), (Integer) entry.getValue()));
                }
            }

            // 2. Migrate Water
            android.content.SharedPreferences waterPrefs = com.example.fitsathi.managers.SecurePrefsManager.getPrefs(this, "WaterPrefs");
            java.util.Map<String, ?> allWater = waterPrefs.getAll();
            for (java.util.Map.Entry<String, ?> entry : allWater.entrySet()) {
                String key = entry.getKey();
                if (key.endsWith("_log")) {
                    String date = key.replace("_log", "");
                    String json = (String) entry.getValue();
                    java.util.List<Long> log = com.example.fitsathi.data.Converters.fromString(json);
                    db.waterLogDao().insertOrUpdate(new com.example.fitsathi.data.entities.WaterLog(date, log));
                }
            }

            migrationPrefs.edit().putBoolean("room_migrated", true).apply();
        });
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "meal_reminder_channel",
                    "Meal Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Reminders for Breakfast, Lunch, Dinner, and Snacks");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
