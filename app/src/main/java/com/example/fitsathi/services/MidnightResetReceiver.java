package com.example.fitsathi.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class MidnightResetReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Reset baseline and goal flag for next day
        SharedPreferences prefs = context.getSharedPreferences("StepPrefs", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("goal_reached_today", false).apply();

        // Broadcast zero steps to UI
        Intent broadcastIntent = new Intent("steps_updated");
        broadcastIntent.putExtra("steps", 0);
        LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);

        // Start StepCounterService again to reschedule midnight alarm
        Intent serviceIntent = new Intent(context, StepCounterService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
