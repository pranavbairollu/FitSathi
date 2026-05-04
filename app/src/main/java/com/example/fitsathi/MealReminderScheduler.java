package com.example.fitsathi;

import android.content.Context;
import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import com.example.fitsathi.receivers.MealAlarmReceiver;

public class MealReminderScheduler {

    private static final String PREF_NAME = "fitsathi_reminder_prefs";

    public static void scheduleDailyReminder(Context context, String meal, int hour24, int minute) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        Intent intent = new Intent(context, MealAlarmReceiver.class);
        intent.putExtra("meal_name", meal);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                getStableRequestCode(meal),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long triggerTime = calculateTriggerTime(hour24, minute);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                // Fallback to inexact if permission not granted
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }

        // Save reminder time for UI and restoration purposes
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(meal + "_hour", hour24)
                .putInt(meal + "_minute", minute)
                .apply();
    }

    public static void cancelReminder(Context context, String meal) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, MealAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                getStableRequestCode(meal),
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }

        // Remove saved reminder time
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(meal + "_hour")
                .remove(meal + "_minute")
                .apply();
    }

    static long calculateTriggerTime(int hour24, int minute) {
        Calendar now = Calendar.getInstance();
        Calendar scheduledTime = Calendar.getInstance();
        scheduledTime.set(Calendar.HOUR_OF_DAY, hour24);
        scheduledTime.set(Calendar.MINUTE, minute);
        scheduledTime.set(Calendar.SECOND, 0);
        scheduledTime.set(Calendar.MILLISECOND, 0);

        if (scheduledTime.before(now)) {
            scheduledTime.add(Calendar.DAY_OF_MONTH, 1);
        }

        return scheduledTime.getTimeInMillis();
    }

    private static int getStableRequestCode(String meal) {
        if (meal == null) return 100;
        switch (meal.toLowerCase()) {
            case "breakfast": return 101;
            case "lunch": return 102;
            case "dinner": return 103;
            case "snacks": return 104;
            default: return 100;
        }
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
