package com.example.fitsathi;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class GoalSettingActivity extends AppCompatActivity {

    private RadioGroup goalRadioGroup;
    private RadioButton rbDefault, rbCustom;
    private EditText customGoalInput;
    private Button saveButton;

    private static final String PREF_NAME = "StepPrefs";
    private static final String KEY_GOAL = "daily_goal";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goal_setting);

        // Link views with XML IDs
        goalRadioGroup = findViewById(R.id.rgGoalChoice);
        rbDefault = findViewById(R.id.rbDefault);
        rbCustom = findViewById(R.id.rbCustom);
        customGoalInput = findViewById(R.id.etCustomGoal);
        saveButton = findViewById(R.id.btnSaveGoal);

        // Load previously saved goal
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        int savedGoal = prefs.getInt(KEY_GOAL, 10000); // Default 10000 if not set

        // Pre-select the correct option
        if (savedGoal == 10000) {
            rbDefault.setChecked(true);
            customGoalInput.setEnabled(false);
        } else {
            rbCustom.setChecked(true);
            customGoalInput.setEnabled(true);
            customGoalInput.setText(String.valueOf(savedGoal));
        }

        // Enable custom input if "Custom" is selected
        goalRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbCustom) {
                customGoalInput.setEnabled(true);
                customGoalInput.requestFocus();
            } else {
                customGoalInput.setEnabled(false);
                customGoalInput.setText(""); // Clear if not custom
            }
        });

        // Save goal button
        saveButton.setOnClickListener(v -> {
            int goal;
            int checkedId = goalRadioGroup.getCheckedRadioButtonId();

            if (checkedId == R.id.rbDefault) {
                goal = 10000;
            } else { // custom
                String input = customGoalInput.getText().toString().trim();
                if (input.isEmpty()) {
                    Toast.makeText(this, "Please enter a custom goal", Toast.LENGTH_SHORT).show();
                    return;
                }
                goal = Integer.parseInt(input);
            }

            // Save to SharedPreferences
            prefs.edit().putInt(KEY_GOAL, goal).apply();

            Toast.makeText(this, "Daily goal set to " + goal + " steps", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
