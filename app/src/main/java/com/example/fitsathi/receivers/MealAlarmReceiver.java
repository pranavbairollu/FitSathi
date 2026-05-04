package com.example.fitsathi.receivers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

import com.example.fitsathi.MealReminderScheduler;
import com.example.fitsathi.R;
import com.example.fitsathi.SplashActivity;

public class MealAlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "meal_reminder_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        String mealName = intent.getStringExtra("meal_name");
        if (mealName == null) return;

        showNotification(context, mealName);

        // Reschedule for next day to maintain the "Periodic" behavior with AlarmManager
        int[] time = MealReminderScheduler.getSavedReminderTime(context, mealName);
        if (time[0] != -1) {
            MealReminderScheduler.scheduleDailyReminder(context, mealName, time[0], time[1]);
        }
    }

    private void showNotification(Context context, String meal) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Meal Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        Intent splashIntent = new Intent(context, SplashActivity.class);
        splashIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, getNotificationId(meal), splashIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Meal Reminder")
                .setContentText("It's time for your " + meal + "!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER);

        notificationManager.notify(getNotificationId(meal), builder.build());
    }

    private int getNotificationId(String meal) {
        if (meal == null) return 1000;
        switch (meal.toLowerCase()) {
            case "breakfast": return 1001;
            case "lunch": return 1002;
            case "dinner": return 1003;
            case "snacks": return 1004;
            default: return 1000;
        }
    }
}
