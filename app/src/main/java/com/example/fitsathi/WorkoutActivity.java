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
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.fitsathi.data.ExerciseLoader;
import com.example.fitsathi.managers.WorkoutHistoryManager;
import com.example.fitsathi.models.Exercise;
import com.example.fitsathi.models.WorkoutSession;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WorkoutActivity extends BaseActivity {

    private LinearLayout workoutListContainer;
    private ProgressBar dailyProgressBar;
    private TextView progressText;

    private WorkoutHistoryManager historyManager;
    private WorkoutSession currentSession;

    private CountDownTimer activeTimer;
    private CountDownTimer restTimer;

    private static final int REST_SECONDS = 30;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout);

        workoutListContainer = findViewById(R.id.workout_list_container);
        dailyProgressBar = findViewById(R.id.daily_progress_bar);
        progressText = findViewById(R.id.progress_text);

        ImageView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        historyManager = new WorkoutHistoryManager(this);

        if (savedInstanceState != null) {
            // Restore from orientation change
            // Actually, we'll rely on the persistent storage in historyManager for bulletproof reliability
        }

        initializeWorkout();
    }

    private void initializeWorkout() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        currentSession = historyManager.loadCurrentSession();

        if (currentSession == null || !today.equals(currentSession.date)) {
            // New day or no session, create new
            SharedPreferences fitnessPrefs = getSecurePrefs("FitnessPrefs");
            String goal = fitnessPrefs.getString("goal", "");
            String level = fitnessPrefs.getString("level", "");
            String location = fitnessPrefs.getString("location", "");
            String timePref = fitnessPrefs.getString("time", "");

            List<Exercise> exercises = ExerciseLoader.loadExercisesForUserPrefs(this, goal, level, location, timePref);
            currentSession = new WorkoutSession(today, exercises);
            historyManager.saveCurrentSession(currentSession);
        }

        renderWorkout();
        updateDailyProgress();
    }

    private void renderWorkout() {
        workoutListContainer.removeAllViews();
        if (currentSession.exercises.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("No workout available for today.");
            emptyView.setGravity(Gravity.CENTER);
            emptyView.setPadding(0, 50, 0, 0);
            workoutListContainer.addView(emptyView);
            return;
        }

        for (int i = 0; i < currentSession.exercises.size(); i++) {
            Exercise ex = currentSession.exercises.get(i);
            boolean isCompleted = currentSession.completedStatus.get(i);
            boolean isActive = (i == currentSession.currentExerciseIndex);
            final int index = i;

            View cardLayout = getLayoutInflater().inflate(R.layout.item_workout_exercise, workoutListContainer, false);
            
            ImageView exerciseImage = cardLayout.findViewById(R.id.exercise_image);
            TextView nameText = cardLayout.findViewById(R.id.exercise_name);
            TextView repsText = cardLayout.findViewById(R.id.exercise_reps);
            TextView timerTv = cardLayout.findViewById(R.id.exercise_timer);
            Button startBtn = cardLayout.findViewById(R.id.btn_start);
            Button pauseBtn = cardLayout.findViewById(R.id.btn_pause);
            Button stopBtn = cardLayout.findViewById(R.id.btn_stop);
            ImageView checkMark = cardLayout.findViewById(R.id.checkmark_icon);

            exerciseImage.setImageResource(ex.imageRes);
            nameText.setText(ex.name);
            repsText.setText(ex.setsReps);

            if (isCompleted) {
                timerTv.setText("Done!");
                startBtn.setVisibility(View.GONE);
                checkMark.setVisibility(View.VISIBLE);
                cardLayout.setAlpha(0.6f);
                cardLayout.setBackgroundResource(R.drawable.exercise_card_bg);
            } else if (isActive) {
                cardLayout.setBackgroundResource(R.drawable.exercise_card_active_bg);
                long secs = currentSession.remainingSeconds;
                timerTv.setText(String.format(Locale.getDefault(), "%02d:%02d", secs / 60, secs % 60));
                
                if (currentSession.isPaused) {
                    startBtn.setVisibility(View.VISIBLE);
                    pauseBtn.setVisibility(View.GONE);
                    stopBtn.setVisibility(View.VISIBLE);
                } else if (activeTimer != null) {
                    startBtn.setVisibility(View.GONE);
                    pauseBtn.setVisibility(View.VISIBLE);
                    stopBtn.setVisibility(View.VISIBLE);
                }
            } else {
                cardLayout.setBackgroundResource(R.drawable.exercise_card_bg);
                timerTv.setText(String.format(Locale.getDefault(), "%02d:00", ex.duration));
            }

            startBtn.setOnClickListener(v -> startExercise(index, cardLayout));
            pauseBtn.setOnClickListener(v -> pauseExercise());
            stopBtn.setOnClickListener(v -> resetExercise(index, cardLayout));

            workoutListContainer.addView(cardLayout);
        }
    }

    private void startExercise(int index, View cardLayout) {
        if (activeTimer != null) activeTimer.cancel();
        
        currentSession.isPaused = false;
        currentSession.currentExerciseIndex = index;
        
        TextView timerTv = cardLayout.findViewById(R.id.exercise_timer);
        Button startBtn = cardLayout.findViewById(R.id.btn_start);
        Button pauseBtn = cardLayout.findViewById(R.id.btn_pause);
        Button stopBtn = cardLayout.findViewById(R.id.btn_stop);

        startBtn.setVisibility(View.GONE);
        pauseBtn.setVisibility(View.VISIBLE);
        stopBtn.setVisibility(View.VISIBLE);

        activeTimer = new CountDownTimer(currentSession.remainingSeconds * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                currentSession.remainingSeconds = millisUntilFinished / 1000;
                timerTv.setText(String.format(Locale.getDefault(), "%02d:%02d", currentSession.remainingSeconds / 60, currentSession.remainingSeconds % 60));
                // Save occasionally to survive app kill
                if (currentSession.remainingSeconds % 5 == 0) {
                    historyManager.saveCurrentSession(currentSession);
                }
            }

            @Override
            public void onFinish() {
                completeExercise(index, cardLayout);
            }
        }.start();
        
        historyManager.saveCurrentSession(currentSession);
    }

    private void pauseExercise() {
        if (activeTimer != null) {
            activeTimer.cancel();
            activeTimer = null;
        }
        currentSession.isPaused = true;
        historyManager.saveCurrentSession(currentSession);
        renderWorkout(); // Refresh UI to show start button again
    }

    private void resetExercise(int index, View cardLayout) {
        if (activeTimer != null) {
            activeTimer.cancel();
            activeTimer = null;
        }
        Exercise ex = currentSession.exercises.get(index);
        currentSession.remainingSeconds = ex.durationSec;
        currentSession.isPaused = true;
        historyManager.saveCurrentSession(currentSession);
        renderWorkout();
    }

    private void completeExercise(int index, View cardLayout) {
        activeTimer = null;
        currentSession.markCompleted(index);
        Exercise ex = currentSession.exercises.get(index);
        historyManager.addCompletedExercise(this, ex);
        
        updateDailyProgress();

        if (index + 1 < currentSession.exercises.size()) {
            currentSession.currentExerciseIndex = index + 1;
            currentSession.remainingSeconds = currentSession.exercises.get(index + 1).durationSec;
            currentSession.isPaused = true; // Auto-pause before next exercise
            historyManager.saveCurrentSession(currentSession);
            
            renderWorkout();
            View nextCard = workoutListContainer.getChildAt(index + 1);
            if (nextCard != null) {
                startRestTimer(nextCard);
            }
        } else {
            // Workout finished!
            currentSession.isPaused = false;
            historyManager.saveCurrentSession(currentSession);
            historyManager.markWorkoutCompleted();
            renderWorkout();
            showCompletionSummary();
        }
    }

    private void startRestTimer(View nextCard) {
        if (restTimer != null) restTimer.cancel();
        
        TextView timerTv = nextCard.findViewById(R.id.exercise_timer);
        Button startBtn = nextCard.findViewById(R.id.btn_start);

        startBtn.setEnabled(false);
        startBtn.setText("Rest");

        restTimer = new CountDownTimer(REST_SECONDS * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long remaining = millisUntilFinished / 1000;
                timerTv.setText(String.format(Locale.getDefault(), "Rest: %ds", remaining));
            }

            @Override
            public void onFinish() {
                renderWorkout(); // Reset views to normal
                restTimer = null;
            }
        }.start();
    }

    private void showCompletionSummary() {
        // Implementation for a summary dialog or view
        Toast.makeText(this, "Workout Complete! Great job!", Toast.LENGTH_LONG).show();
    }

    private void updateDailyProgress() {
        int total = currentSession.exercises.size();
        int completed = 0;
        for (boolean status : currentSession.completedStatus) {
            if (status) completed++;
        }
        
        int progress = total == 0 ? 0 : (int) ((completed * 100.0f) / total);
        dailyProgressBar.setProgress(progress);
        progressText.setText(String.format(Locale.getDefault(), "%d%% Completed (%d/%d)", progress, completed, total));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (activeTimer != null) {
            // We don't necessarily stop it here if it's a foreground service,
            // but since this app uses simple timers, we should save state.
            historyManager.saveCurrentSession(currentSession);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (activeTimer != null) activeTimer.cancel();
        if (restTimer != null) restTimer.cancel();
    }
}
