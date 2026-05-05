package com.example.fitsathi.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;

/**
 * SecurePrefsManager provides a hardened wrapper around SharedPreferences using AES-256 encryption.
 * It uses Android Keystore for key management.
 */
public class SecurePrefsManager {

    private static final String TAG = "SecurePrefsManager";
    private static final String SECURE_PREFIX = "secure_";

    /**
     * Gets an instance of EncryptedSharedPreferences.
     * @param context Application or Activity context.
     * @param fileName The name of the preference file.
     * @return A secure SharedPreferences instance, or normal SharedPreferences if encryption fails.
     */
    public static SharedPreferences getPrefs(Context context, String fileName) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            return EncryptedSharedPreferences.create(
                    context,
                    SECURE_PREFIX + fileName,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "Failed to create EncryptedSharedPreferences for " + fileName, e);
            // Fallback to normal SharedPreferences in case of failure (rare but possible on older devices/modified ROMs)
            return context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        }
    }

    /**
     * Migrates data from an existing plain-text SharedPreferences file to a secure one.
     * @param context Application context.
     * @param fileName The name of the plain-text preference file.
     */
    public static void migrate(Context context, String fileName) {
        SharedPreferences oldPrefs = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        Map<String, ?> allData = oldPrefs.getAll();

        if (allData.isEmpty()) return;

        Log.d(TAG, "Migrating " + allData.size() + " entries for " + fileName);
        SharedPreferences securePrefs = getPrefs(context, fileName);
        SharedPreferences.Editor editor = securePrefs.edit();

        for (Map.Entry<String, ?> entry : allData.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof String) {
                editor.putString(key, (String) value);
            } else if (value instanceof Boolean) {
                editor.putBoolean(key, (Boolean) value);
            } else if (value instanceof Integer) {
                editor.putInt(key, (Integer) value);
            } else if (value instanceof Long) {
                editor.putLong(key, (Long) value);
            } else if (value instanceof Float) {
                editor.putFloat(key, (Float) value);
            }
        }

        if (editor.commit()) {
            // Clear the old unencrypted file to prevent data leakage
            oldPrefs.edit().clear().apply();
            Log.d(TAG, "Successfully migrated and cleared plain-text prefs for " + fileName);
        }
    }
}
