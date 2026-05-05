package com.example.fitsathi.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import com.example.fitsathi.managers.SecurePrefsManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class MidnightResetReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Reset baseline and goal flag for next day in unified prefs
        SharedPreferences prefs = SecurePrefsManager.getPrefs(context, "StepCounterPrefs");
        prefs.edit()
            .putInt("daily_steps", 0)
            .putString("date", new java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(new java.util.Date()))
            .apply();

        // Inform StepCounterService to reset its internal state
        Intent serviceIntent = new Intent(context, StepCounterService.class);
        serviceIntent.setAction("RESET_STEPS");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
