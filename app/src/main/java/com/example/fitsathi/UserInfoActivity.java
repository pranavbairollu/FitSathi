package com.example.fitsathi;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.example.fitsathi.managers.UserInfoManager;

public class UserInfoActivity extends AppCompatActivity {

    private TextInputEditText nameInput, ageInput, heightInput, weightInput;
    private AutoCompleteTextView genderSpinner, activitySpinner;
    private Button saveButton;
    private MaterialToolbar toolbar;
    private TextInputLayout activityLevelLayout;
    private android.widget.ProgressBar progressBar;

    private static final String[] GENDER_OPTIONS = {"Male", "Female", "Other"};
    private static final String[] ACTIVITY_LEVEL_OPTIONS = {
            "Sedentary", "Lightly Active", "Moderately Active", "Very Active", "Extra Active"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);

        toolbar = findViewById(R.id.toolbar);
        nameInput = findViewById(R.id.input_name);
        ageInput = findViewById(R.id.input_age);
        heightInput = findViewById(R.id.input_height);
        weightInput = findViewById(R.id.input_weight);
        genderSpinner = findViewById(R.id.spinner_gender);
        activitySpinner = findViewById(R.id.spinner_activity_level);
        saveButton = findViewById(R.id.save_button);
        activityLevelLayout = findViewById(R.id.activity_level_layout);
        progressBar = findViewById(R.id.user_info_progress);

        toolbar.setNavigationOnClickListener(v -> finish());

        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, GENDER_OPTIONS);
        genderSpinner.setAdapter(genderAdapter);
        genderSpinner.setOnClickListener(v -> genderSpinner.showDropDown());

        ArrayAdapter<String> activityAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, ACTIVITY_LEVEL_OPTIONS);
        activitySpinner.setAdapter(activityAdapter);
        activitySpinner.setOnClickListener(v -> activitySpinner.showDropDown());

        activityLevelLayout.setEndIconOnClickListener(v -> showActivityLevelInfoDialog());

        saveButton.setOnClickListener(v -> {
            if (isInputValid()) {
                saveUserInfo();
            }
        });
    }

    private void showActivityLevelInfoDialog() {
        String[] explanations = {
                "Sedentary: Little or no exercise, desk job.",
                "Lightly Active: Light exercise/sports 1-3 days/week.",
                "Moderately Active: Moderate exercise/sports 3-5 days/week.",
                "Very Active: Hard exercise/sports 6-7 days a week.",
                "Extra Active: Very hard exercise/sports & a physical job."
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle("Activity Level Descriptions")
                .setItems(explanations, null)
                .setPositiveButton("Got it", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private boolean isInputValid() {
        String name = nameInput.getText().toString().trim();
        String ageStr = ageInput.getText().toString().trim();
        String heightStr = heightInput.getText().toString().trim();
        String weightStr = weightInput.getText().toString().trim();
        String gender = genderSpinner.getText().toString();
        String activity = activitySpinner.getText().toString();

        // --- Field Empty Checks ---
        if (name.isEmpty()) {
            nameInput.setError("Name cannot be empty");
            nameInput.requestFocus();
            return false;
        }
        if (ageStr.isEmpty()) {
            ageInput.setError("Age cannot be empty");
            ageInput.requestFocus();
            return false;
        }
        if (heightStr.isEmpty()) {
            heightInput.setError("Height cannot be empty");
            heightInput.requestFocus();
            return false;
        }
        if (weightStr.isEmpty()) {
            weightInput.setError("Weight cannot be empty");
            weightInput.requestFocus();
            return false;
        }
        if (gender.isEmpty()) {
            genderSpinner.setError("Please select a gender");
            genderSpinner.requestFocus();
            return false;
        }
        if (activity.isEmpty()) {
            activitySpinner.setError("Please select an activity level");
            activitySpinner.requestFocus();
            return false;
        }

        // --- Range and Logic Checks ---
        try {
            int age = Integer.parseInt(ageStr);
            if (age < 10 || age > 120) {
                ageInput.setError("Please enter a realistic age (10-120)");
                ageInput.requestFocus();
                return false;
            }

            float height = Float.parseFloat(heightStr);
            if (height < 100 || height > 250) {
                heightInput.setError("Please enter a realistic height in cm (100-250)");
                heightInput.requestFocus();
                return false;
            }

            float weight = Float.parseFloat(weightStr);
            if (weight < 30 || weight > 300) {
                weightInput.setError("Please enter a realistic weight in kg (30-300)");
                weightInput.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers for age, height, and weight", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Clear any previous errors if validation passes
        nameInput.setError(null);
        ageInput.setError(null);
        heightInput.setError(null);
        weightInput.setError(null);
        genderSpinner.setError(null);
        activitySpinner.setError(null);

        return true;
    }

    private void saveUserInfo() {
        String name = nameInput.getText().toString().trim();
        int age = Integer.parseInt(ageInput.getText().toString().trim());
        float height = Float.parseFloat(heightInput.getText().toString().trim());
        float weight = Float.parseFloat(weightInput.getText().toString().trim());
        String gender = genderSpinner.getText().toString();
        String activity = activitySpinner.getText().toString();

        try {
            UserInfoManager.UserInfo newUserInfo = new UserInfoManager.UserInfo();
            newUserInfo.setName(name);
            newUserInfo.setAge(age);
            newUserInfo.setHeight(height);
            newUserInfo.setWeight(weight);
            newUserInfo.setGender(gender);
            newUserInfo.setActivityLevel(activity);

            setLoading(true);
            UserInfoManager.saveUserInfo(newUserInfo, success -> {
                setLoading(false);
                if (success) {
                    Toast.makeText(this, "Info saved successfully!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(UserInfoActivity.this, FitnessGoalActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "Failed to save info. Check your connection.", Toast.LENGTH_LONG).show();
                }
            });

        } catch (Exception e) {
            setLoading(false);
            e.printStackTrace();
            Toast.makeText(this, "An unexpected error occurred while saving", Toast.LENGTH_SHORT).show();
        }
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? android.view.View.VISIBLE : android.view.View.GONE);
        saveButton.setEnabled(!isLoading);
        nameInput.setEnabled(!isLoading);
        ageInput.setEnabled(!isLoading);
        heightInput.setEnabled(!isLoading);
        weightInput.setEnabled(!isLoading);
        genderSpinner.setEnabled(!isLoading);
        activitySpinner.setEnabled(!isLoading);
    }
}
