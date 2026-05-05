package com.example.fitsathi;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import com.example.fitsathi.managers.SecurePrefsManager;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Base activity that handles global configuration such as locale and theme consistency.
 * All activities in the app should extend this class.
 */
public abstract class BaseActivity extends AppCompatActivity {

    protected SharedPreferences getSecurePrefs(String name) {
        return SecurePrefsManager.getPrefs(this, name);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        // Apply the saved locale before the activity is created
        super.attachBaseContext(LocaleHelper.setLocale(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Additional global activity setup can go here
    }
}
