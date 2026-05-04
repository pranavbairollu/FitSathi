package com.example.fitsathi;

import android.app.Application;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class FitSathiApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
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
