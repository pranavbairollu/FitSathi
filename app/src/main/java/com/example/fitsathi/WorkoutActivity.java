package com.example.fitsathi;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.fitsathi.data.ExerciseLoader;
import com.example.fitsathi.managers.WorkoutHistoryManager;
import com.example.fitsathi.models.Exercise;

import java.util.List;
import java.util.Locale;

public class WorkoutActivity extends AppCompatActivity {

    private LinearLayout workoutListContainer;
    private ProgressBar dailyProgressBar;
    private TextView progressText;

    private WorkoutHistoryManager historyManager;

    private List<Exercise> todayExercises;
    private int totalExercisesToday = 0;
    private int completedExercisesToday = 0;

    private static final int REST_SECONDS = 30;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Use the layout from XML for better maintainability
        setContentView(R.layout.activity_workout);

        // Initialize views from the layout
        workoutListContainer = findViewById(R.id.workout_list_container);
        dailyProgressBar = findViewById(R.id.daily_progress_bar);
        progressText = findViewById(R.id.progress_text);

        // Back button
        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        historyManager = new WorkoutHistoryManager(this);

        loadTodayWorkout();
    }

    // ==============================================================
    // ✅ THIS IS THE CORRECTED METHOD
    // ==============================================================
    private void loadTodayWorkout() {
        completedExercisesToday = 0;
        workoutListContainer.removeAllViews(); // Clear previous views

        // Step 1: Get user's saved preferences
        SharedPreferences fitnessPrefs = getSharedPreferences("FitnessPrefs", Context.MODE_PRIVATE);
        String goal = fitnessPrefs.getString("goal", "");
        String level = fitnessPrefs.getString("level", "");
        String location = fitnessPrefs.getString("location", "");
        String timePref = fitnessPrefs.getString("time", "");

        // Step 2: Call the correct method with the preferences
        todayExercises = ExerciseLoader.loadExercisesForUserPrefs(this, goal, level, location, timePref);
        totalExercisesToday = todayExercises.size();

        // Update progress text initially
        updateDailyProgress();

        if (todayExercises.isEmpty()) {
            // Handle case where no exercises are found
            TextView emptyView = new TextView(this);
            emptyView.setText("No workout available for today. Please adjust your preferences.");
            emptyView.setGravity(Gravity.CENTER);
            emptyView.setPadding(0, 50, 0, 0);
            workoutListContainer.addView(emptyView);
            return;
        }

        // The rest of the logic remains the same
        for (int i = 0; i < todayExercises.size(); i++) {
            Exercise ex = todayExercises.get(i);
            final int index = i;

            // Inflate card from layout instead of creating programmatically
            View cardLayout = getLayoutInflater().inflate(R.layout.item_workout_exercise, workoutListContainer, false);

            // Find views within the inflated card
            ImageView exerciseImage = cardLayout.findViewById(R.id.exercise_image);
            TextView nameText = cardLayout.findViewById(R.id.exercise_name);
            TextView repsText = cardLayout.findViewById(R.id.exercise_reps);
            TextView timerTv = cardLayout.findViewById(R.id.exercise_timer);
            Button startBtn = cardLayout.findViewById(R.id.btn_start);
            Button pauseBtn = cardLayout.findViewById(R.id.btn_pause);
            Button stopBtn = cardLayout.findViewById(R.id.btn_stop);
            ImageView checkMark = cardLayout.findViewById(R.id.checkmark_icon);

            // Populate the card with exercise data
            exerciseImage.setImageResource(ex.imageRes);
            nameText.setText(ex.name);
            repsText.setText(ex.setsReps);
            timerTv.setText(String.format(Locale.getDefault(),"%02d:00", ex.duration));


            workoutListContainer.addView(cardLayout);

            // Highlight the first exercise
            if (i == 0) {
                cardLayout.setBackgroundResource(R.drawable.exercise_card_active_bg);
            }

            final CountDownTimer[] timer = new CountDownTimer[1];

            startBtn.setOnClickListener(v -> {
                startBtn.setVisibility(View.GONE);
                pauseBtn.setVisibility(View.VISIBLE);
                stopBtn.setVisibility(View.VISIBLE);

                // Use a member variable for remaining time to handle pauses correctly
                long durationMillis = (ex.durationSec * 1000L);

                timer[0] = new CountDownTimer(durationMillis, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        long remainingSeconds = millisUntilFinished / 1000;
                        timerTv.setText(String.format(Locale.getDefault(), "%02d:%02d", remainingSeconds / 60, remainingSeconds % 60));
                    }

                    @Override
                    public void onFinish() {
                        timerTv.setText("Done!");
                        pauseBtn.setVisibility(View.GONE);
                        stopBtn.setVisibility(View.GONE);
                        checkMark.setVisibility(View.VISIBLE);
                        checkMark.setImageResource(R.drawable.ic_checkmark_done); // Use a custom checkmark drawable

                        cardLayout.animate().alpha(0.6f).setDuration(500).start();
                        cardLayout.setBackgroundResource(R.drawable.exercise_card_bg);


                        historyManager.addWorkout(ex);
                        completedExercisesToday++;
                        updateDailyProgress();

                        // Activate the next card and start rest timer
                        if (index + 1 < totalExercisesToday) {
                            View nextCard = workoutListContainer.getChildAt(index + 1);
                            if (nextCard != null) {
                                nextCard.setBackgroundResource(R.drawable.exercise_card_active_bg);
                                startRestTimer(nextCard);
                            }
                        }
                    }
                }.start();
            });

            pauseBtn.setOnClickListener(v -> {
                // This pause logic is basic. For a real app, you'd need to save remaining time.
                if (timer[0] != null) {
                    timer[0].cancel();
                    startBtn.setVisibility(View.VISIBLE);
                    pauseBtn.setVisibility(View.GONE);
                }
            });

            stopBtn.setOnClickListener(v -> {
                if (timer[0] != null) {
                    timer[0].cancel();
                    timerTv.setText(String.format(Locale.getDefault(),"%02d:00", ex.duration));
                    startBtn.setVisibility(View.VISIBLE);
                    pauseBtn.setVisibility(View.GONE);
                    stopBtn.setVisibility(View.GONE);
                }
            });
        }
    }


    private void startRestTimer(View nextCard) {
        TextView timerTv = nextCard.findViewById(R.id.exercise_timer);
        Button startBtn = nextCard.findViewById(R.id.btn_start);

        // Disable start button during rest
        startBtn.setEnabled(false);
        startBtn.setText("Rest");

        new CountDownTimer(REST_SECONDS * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long remaining = millisUntilFinished / 1000;
                timerTv.setText(String.format(Locale.getDefault(), "Rest: %ds", remaining));
            }

            @Override
            public void onFinish() {
                // Find the original duration from the exercise model to reset the text
                Exercise nextExercise = findExerciseForCard(nextCard);
                if (nextExercise != null) {
                    timerTv.setText(String.format(Locale.getDefault(),"%02d:00", nextExercise.duration));
                }
                startBtn.setEnabled(true);
                startBtn.setText("Start");
            }
        }.start();
    }

    // Helper to find which exercise a card view corresponds to
    private Exercise findExerciseForCard(View cardView) {
        int index = workoutListContainer.indexOfChild(cardView);
        if (index != -1 && index < todayExercises.size()) {
            return todayExercises.get(index);
        }
        return null;
    }


    private void updateDailyProgress() {
        int progress = totalExercisesToday == 0 ? 0 :
                (int) ((completedExercisesToday * 100.0f) / totalExercisesToday);
        dailyProgressBar.setProgress(progress);
        progressText.setText(String.format(Locale.getDefault(), "%d%% Completed (%d/%d)", progress, completedExercisesToday, totalExercisesToday));
    }
}
