package com.example.fitsathi.managers;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.fitsathi.R;
import com.google.firebase.auth.FirebaseAuth;
import java.io.File;

/**
 * Manager responsible for resetting all local application data.
 * This is used for the "Global Reset" feature.
 */
public class ResetManager {

    /**
     * Performs a full local data reset.
     * 1. Clears all SharedPreferences listed in R.array.preference_files.
     * 2. Signs out from Firebase.
     * 3. Clears internal application cache.
     */
    public static void performGlobalReset(Context context) {
        // 1. Clear SharedPreferences
        String[] prefNames = context.getResources().getStringArray(R.array.preference_files);
        for (String prefName : prefNames) {
            // Clear plain-text prefs
            context.getSharedPreferences(prefName, Context.MODE_PRIVATE).edit().clear().commit();
            // Clear secure prefs
            SecurePrefsManager.getPrefs(context, prefName).edit().clear().commit();
        }

        // 2. Sign out from Firebase
        FirebaseAuth.getInstance().signOut();

        // 3. Cancel all alarms
        com.example.fitsathi.MealReminderScheduler.cancelAllReminders(context);

        // 4. Clear Room Database (Async)
        new Thread(() -> {
            com.example.fitsathi.data.AppDatabase.getDatabase(context).clearAllTables();
        }).start();

        // 5. Clear Cache
        deleteCache(context);
    }

    private static void deleteCache(Context context) {
        try {
            File dir = context.getCacheDir();
            deleteDir(dir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = deleteDir(new File(dir, child));
                    if (!success) {
                        return false;
                    }
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }
}
