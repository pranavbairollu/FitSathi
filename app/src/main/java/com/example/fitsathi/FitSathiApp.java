package com.example.fitsathi;

import android.app.Application;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class FitSathiApp extends Application {

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
    }

    private void applySavedTheme() {
        android.content.SharedPreferences prefs = getSharedPreferences(
                getString(R.string.settings_prefs_name),
                MODE_PRIVATE
        );
        boolean darkMode = prefs.getBoolean(getString(R.string.dark_mode_enabled_key), false);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                darkMode ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
        );
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
