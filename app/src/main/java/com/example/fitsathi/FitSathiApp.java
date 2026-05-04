package com.example.fitsathi;

import android.app.Application;

public class FitSathiApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        restoreAlarms();
    }

    private void restoreAlarms() {
        String[] meals = {"Breakfast", "Lunch", "Dinner", "Snacks"};
        for (String meal : meals) {
            int[] time = MealReminderScheduler.getSavedReminderTime(this, meal);
            if (time != null && time[0] != -1) {
                MealReminderScheduler.scheduleDailyReminder(this, meal, time[0], time[1]);
            }
        }
    }
}
