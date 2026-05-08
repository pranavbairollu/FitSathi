package com.example.fitsathi;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.fitsathi.fragments.BarChartFragment;
import com.example.fitsathi.fragments.HomeFragment;
import com.example.fitsathi.fragments.ProfileFragment;
import com.example.fitsathi.fragments.SettingsFragment;
import com.example.fitsathi.fragments.StepsChartFragment;
import com.example.fitsathi.fragments.TrackerFragment;
import com.example.fitsathi.fragments.WaterFragment;
import com.example.fitsathi.fragments.WeightTrackerFragment;
import com.example.fitsathi.services.StepCounterService;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DashboardActivity extends BaseActivity {

    private BottomNavigationView bottomNavigationView;
    private static final int PERMISSION_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Splash Screen
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Permissions & Services
        checkAndRequestPermissions();
        startStepService();

        // Bottom Navigation
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_tracker) {
                selectedFragment = new TrackerFragment();
            } else if (itemId == R.id.nav_weight) {
                selectedFragment = new WeightTrackerFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            } else if (itemId == R.id.nav_community) {
                selectedFragment = new com.example.fitsathi.fragments.SquadsFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                openFragment(selectedFragment, false);
            }

            return true;
        });


        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }

        setupHealthSync();
    }

    // -----------------------------
    // Permissions Handling
    // -----------------------------
    private void checkAndRequestPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();

        // Activity recognition (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION);
        }

        // Notifications (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Loop through all granted permissions
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.ACTIVITY_RECOGNITION) && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    // If activity recognition was granted, start the step service
                    startStepService();
                    break; // No need to check further
                }
            }
        }
    }

    // -----------------------------
    // Step Counter Service
    // -----------------------------
    private void startStepService() {
        Intent intent = new Intent(this, StepCounterService.class);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                ContextCompat.startForegroundService(this, intent);
            else startService(intent);
        }
    }

    // -----------------------------
    // Fragment Navigation
    // -----------------------------
    private void openFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        if (addToBackStack) transaction.addToBackStack(null);
        transaction.commit();
    }

    // -----------------------------
    // Public Navigation Helpers
    // -----------------------------
    public void openTrackerFragment() {
        bottomNavigationView.setSelectedItemId(R.id.nav_tracker);
    }

    public void openBarChartFragment() {
        openFragment(new BarChartFragment(), true);
    }

    public void openStepsChartFragment() {
        openFragment(new StepsChartFragment(), true);
    }

    public void openProfileFragment() {
        bottomNavigationView.setSelectedItemId(R.id.nav_profile);
    }

    public void openUserInfoActivity() {
        startActivity(new Intent(this, UserInfoActivity.class));
    }

    public void openWaterFragment() {
        openFragment(new WaterFragment(), true);
    }

    private void setupHealthSync() {
        com.example.fitsathi.managers.HealthConnectManager.getInstance(this).checkPermissions(allGranted -> {
            if (allGranted) {
                androidx.work.PeriodicWorkRequest syncRequest =
                        new androidx.work.PeriodicWorkRequest.Builder(com.example.fitsathi.workers.HealthSyncWorker.class, 4, java.util.concurrent.TimeUnit.HOURS)
                                .setConstraints(new androidx.work.Constraints.Builder()
                                        .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                                        .build())
                                .build();

                androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                        "HealthConnectSync",
                        androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                        syncRequest
                );
                android.util.Log.d("DashboardActivity", "Health Connect periodic sync scheduled.");
            }
        });
    }

    public void openHealthSyncFragment() {
        openFragment(new com.example.fitsathi.fragments.HealthSyncFragment(), true);
    }

    public void openSettingsFragment() {
        openFragment(new SettingsFragment(), true);
    }
}
