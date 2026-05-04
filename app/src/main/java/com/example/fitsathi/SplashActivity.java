package com.example.fitsathi;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.splashscreen.SplashScreen; // ✅ AndroidX SplashScreen
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.example.fitsathi.managers.UserInfoManager;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Install SplashScreen BEFORE anything else
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);

        // Apply saved theme
        applySavedTheme();

        // Navigation logic
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            UserInfoManager.getUserInfo(userInfo -> {
                if (userInfo == null || userInfo.getName() == null) {
                    startActivity(new Intent(this, UserInfoActivity.class));
                } else if (userInfo.getFitnessGoal() == null || userInfo.getFitnessGoal().isEmpty()) {
                    startActivity(new Intent(this, FitnessGoalActivity.class));
                } else {
                    startActivity(new Intent(this, DashboardActivity.class));
                }
                finish();
            });
        } else {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    private void applySavedTheme() {
        SharedPreferences prefs = getSharedPreferences(getString(R.string.settings_prefs_name), MODE_PRIVATE);
        boolean darkMode = prefs.getBoolean(getString(R.string.dark_mode_enabled_key), false);
        AppCompatDelegate.setDefaultNightMode(
                darkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }
}
