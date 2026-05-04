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

public class FitnessGoalActivity extends AppCompatActivity {

    private Spinner spinnerGoal, spinnerLevel, spinnerLocation, spinnerTime;
    private Button btnSaveGoal;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fitness_goal);

        spinnerGoal = findViewById(R.id.spinner_goal);
        spinnerLevel = findViewById(R.id.spinner_level);
        spinnerLocation = findViewById(R.id.spinner_location);
        spinnerTime = findViewById(R.id.spinner_time);
        btnSaveGoal = findViewById(R.id.btn_save_goal);

        // ----------------------------
        // Setup spinner data
        // -----------------------------
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
        String goal = getSharedPreferences("FitnessPrefs", Context.MODE_PRIVATE)
                .getString("goal", "Select your Goal");
        String level = getSharedPreferences("FitnessPrefs", Context.MODE_PRIVATE)
                .getString("level", "Select Fitness Level");
        String location = getSharedPreferences("FitnessPrefs", Context.MODE_PRIVATE)
                .getString("location", "Select Workout Location");
        String time = getSharedPreferences("FitnessPrefs", Context.MODE_PRIVATE)
                .getString("time", "Select Workout Duration");

        setSpinnerSelection(spinnerGoal, goal);
        setSpinnerSelection(spinnerLevel, level);
        setSpinnerSelection(spinnerLocation, location);

        // Map stored "Short/Medium/Long" back to full spinner text
        if (time.equals("Short")) time = "Short (10-20 min)";
        else if (time.equals("Medium")) time = "Medium (20-40 min)";
        else if (time.equals("Long")) time = "Long (40-60 min)";

        setSpinnerSelection(spinnerTime, time);
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

        getSharedPreferences("FitnessPrefs", Context.MODE_PRIVATE).edit()
                .putString("goal", goal)
                .putString("level", level)
                .putString("location", location)
                .putString("time", timePref)
                .apply();

        // Clear the saved daily workout so a new one is generated
        getSharedPreferences("DailyWorkoutPrefs", Context.MODE_PRIVATE).edit()
                .remove("workout_date")
                .remove("workout_names")
                .apply();

        Toast.makeText(this, "Goal saved!", Toast.LENGTH_SHORT).show();

        // Return to dashboard
        startActivity(new Intent(this, DashboardActivity.class));
        finish();
    }
}
