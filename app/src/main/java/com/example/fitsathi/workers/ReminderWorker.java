package com.example.fitsathi.workers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.fitsathi.R;
import com.example.fitsathi.SplashActivity;

public class ReminderWorker extends Worker {

    public static final String KEY_MEAL_NAME = "key_meal_name";
    private static final String CHANNEL_ID = "meal_reminder_channel";

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String mealName = getInputData().getString(KEY_MEAL_NAME);
        if (mealName == null) {
            return Result.failure();
        }

        showNotification(getApplicationContext(), mealName);

        return Result.success();
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

        Intent intent = new Intent(context, SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Meal Reminder")
                .setContentText("It's time for your " + meal + "!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify(getStableRequestCode(meal), builder.build());
    }

    private int getStableRequestCode(String meal) {
        if (meal == null) return 0;
        switch (meal.toLowerCase()) {
            case "breakfast": return 1;
            case "lunch": return 2;
            case "dinner": return 3;
            case "snacks": return 4;
            default: return 0;
        }
    }
}
