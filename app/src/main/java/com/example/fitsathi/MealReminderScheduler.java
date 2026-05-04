package com.example.fitsathi;

import android.content.Context;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.fitsathi.workers.ReminderWorker;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class MealReminderScheduler {

    private static final String PREF_NAME = "fitsathi_reminder_prefs";

    public static void scheduleDailyReminder(Context context, String meal, int hour24, int minute) {
        WorkManager workManager = WorkManager.getInstance(context);

        Data inputData = new Data.Builder()
                .putString(ReminderWorker.KEY_MEAL_NAME, meal)
                .build();

        long initialDelay = calculateInitialDelay(hour24, minute);

        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(ReminderWorker.class, 24, TimeUnit.HOURS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .build();

        workManager.enqueueUniquePeriodicWork(meal, ExistingPeriodicWorkPolicy.REPLACE, workRequest);

        // Save reminder time for UI purposes
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(meal + "_hour", hour24)
                .putInt(meal + "_minute", minute)
                .apply();
    }

    public static void cancelReminder(Context context, String meal) {
        WorkManager.getInstance(context).cancelUniqueWork(meal);

        // Remove saved reminder time
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(meal + "_hour")
                .remove(meal + "_minute")
                .apply();
    }

    private static long calculateInitialDelay(int hour24, int minute) {
        Calendar now = Calendar.getInstance();
        Calendar scheduledTime = Calendar.getInstance();
        scheduledTime.set(Calendar.HOUR_OF_DAY, hour24);
        scheduledTime.set(Calendar.MINUTE, minute);
        scheduledTime.set(Calendar.SECOND, 0);

        if (scheduledTime.before(now)) {
            scheduledTime.add(Calendar.DAY_OF_MONTH, 1);
        }

        return scheduledTime.getTimeInMillis() - now.getTimeInMillis();
    }

    public static boolean isReminderScheduled(Context context, String meal) {
        return getSavedReminderTime(context, meal)[0] != -1;
    }

    public static int[] getSavedReminderTime(Context context, String meal) {
        int h = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getInt(meal + "_hour", -1);
        int m = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getInt(meal + "_minute", -1);
        return new int[]{h, m};
    }
}
