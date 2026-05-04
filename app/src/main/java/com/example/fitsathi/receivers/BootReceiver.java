package com.example.fitsathi.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.fitsathi.MealReminderScheduler;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
            Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            
            Log.d(TAG, "Restoring meal reminders after reboot/update");
            restoreAlarms(context);
        }
    }

    private void restoreAlarms(Context context) {
        String[] meals = {"Breakfast", "Lunch", "Dinner", "Snacks"};
        for (String meal : meals) {
            int[] time = MealReminderScheduler.getSavedReminderTime(context, meal);
            if (time[0] != -1) {
                MealReminderScheduler.scheduleDailyReminder(context, meal, time[0], time[1]);
            }
        }
    }
}
