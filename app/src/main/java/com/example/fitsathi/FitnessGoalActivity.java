package com.example.fitsathi;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fitsathi.managers.UserInfoManager;

import java.util.HashMap;
import java.util.Map;

public class FitnessGoalActivity extends BaseActivity {

    private Spinner spinnerGoal, spinnerLevel, spinnerLocation, spinnerTime;
    private Button btnSaveGoal;
    private android.widget.ProgressBar progressBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fitness_goal);

        spinnerGoal = findViewById(R.id.spinner_goal);
        spinnerLevel = findViewById(R.id.spinner_level);
        spinnerLocation = findViewById(R.id.spinner_location);
        spinnerTime = findViewById(R.id.spinner_time);
        btnSaveGoal = findViewById(R.id.btn_save_goal);
        progressBar = findViewById(R.id.fitness_goal_progress);

        // ... (ArrayAdapter setups) ...
        ArrayAdapter<String> goalAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Select your Goal", "Lose Weight", "Build Muscle", "Tone"});
        goalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGoal.setAdapter(goalAdapter);

        ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Select Fitness Level", "Beginner", "Intermediate", "Advanced"});
        levelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLevel.setAdapter(levelAdapter);

        ArrayAdapter<String> locationAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Select Workout Location", "Home", "Gym"});
        locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLocation.setAdapter(locationAdapter);

        ArrayAdapter<String> timeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Select Workout Duration",
                        "Short (10-20 min)",
                        "Medium (20-40 min)",
                        "Long (40-60 min)"});
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTime.setAdapter(timeAdapter);

        // Load saved values from SharedPreferences
        loadSavedGoal();

        btnSaveGoal.setOnClickListener(v -> saveGoal());
    }

    private void loadSavedGoal() {
        setLoading(true);
        UserInfoManager.getUserInfo(userInfo -> {
            setLoading(false);
            if (userInfo != null) {
                if (userInfo.getFitnessGoal() != null) setSpinnerSelection(spinnerGoal, userInfo.getFitnessGoal());
                if (userInfo.getDifficultyLevel() != null) setSpinnerSelection(spinnerLevel, userInfo.getDifficultyLevel());
                if (userInfo.getWorkoutLocation() != null) setSpinnerSelection(spinnerLocation, userInfo.getWorkoutLocation());

                String time = userInfo.getWorkoutDuration();
                if (time != null) {
                    if (time.equals("Short")) time = "Short (10-20 min)";
                    else if (time.equals("Medium")) time = "Medium (20-40 min)";
                    else if (time.equals("Long")) time = "Long (40-60 min)";
                    setSpinnerSelection(spinnerTime, time);
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void setSpinnerSelection(Spinner spinner, String value) {
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();
        int position = adapter.getPosition(value);
        if (position >= 0) spinner.setSelection(position);
    }

    private void saveGoal() {
        String goal = spinnerGoal.getSelectedItem().toString();
        String level = spinnerLevel.getSelectedItem().toString();
        String location = spinnerLocation.getSelectedItem().toString();
        String time = spinnerTime.getSelectedItem().toString();

        if (goal.equals("Select your Goal") || level.equals("Select Fitness Level") ||
                location.equals("Select Workout Location") || time.equals("Select Workout Duration")) {
            Toast.makeText(this, "Please select all options before saving.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Store only Short/Medium/Long for filtering
        String timePref = time.startsWith("Short") ? "Short" :
                time.startsWith("Medium") ? "Medium" : "Long";

        Map<String, Object> updates = new HashMap<>();
        updates.put("fitnessGoal", goal);
        updates.put("difficultyLevel", level);
        updates.put("workoutLocation", location);
        updates.put("workoutDuration", timePref);

        setLoading(true);
        UserInfoManager.updateUserInfo(updates, success -> {
            setLoading(false);
            if (success) {
                // Clear local daily workout so a new one is generated
                getSecurePrefs("DailyWorkoutPrefs").edit()
                        .remove("workout_date")
                        .remove("workout_names")
                        .apply();

                Toast.makeText(this, "Goal saved and synced!", Toast.LENGTH_SHORT).show();

                // Return to dashboard
                Intent intent = new Intent(this, DashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Failed to sync goals. Check your connection.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? android.view.View.VISIBLE : android.view.View.GONE);
        btnSaveGoal.setEnabled(!isLoading);
        spinnerGoal.setEnabled(!isLoading);
        spinnerLevel.setEnabled(!isLoading);
        spinnerLocation.setEnabled(!isLoading);
        spinnerTime.setEnabled(!isLoading);
    }
}
