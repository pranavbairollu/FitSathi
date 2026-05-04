package com.example.fitsathi.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.fitsathi.R;
import com.example.fitsathi.data.ExerciseLoader;
import com.example.fitsathi.managers.WorkoutHistoryManager;
import com.example.fitsathi.models.Exercise;

import java.util.List;

public class WorkoutFragment extends Fragment {

    private LinearLayout workoutListContainer;
    private ProgressBar dailyProgressBar;
    private TextView progressText;
    private WorkoutHistoryManager historyManager;

    private List<Exercise> todayExercises;
    private int totalExercisesToday = 0;
    private int completedExercisesToday = 0;
    private static final int REST_SECONDS = 30;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_workout, container, false);

        workoutListContainer = view.findViewById(R.id.workout_list_container);
        dailyProgressBar = view.findViewById(R.id.daily_progress_bar);
        progressText = view.findViewById(R.id.progress_text);
        historyManager = new WorkoutHistoryManager(requireContext());

        loadTodayWorkout();
        return view;
    }

    private void loadTodayWorkout() {
        workoutListContainer.removeAllViews();
        completedExercisesToday = 0;

        Context context = requireContext();
        SharedPreferences prefs = context.getSharedPreferences("FitnessPrefs", Context.MODE_PRIVATE);
        String goal = prefs.getString("goal", "");
        String level = prefs.getString("level", "");
        String location = prefs.getString("location", "");
        String timePref = prefs.getString("time", "");

        // Load exercises filtered by user's saved preferences
        todayExercises = ExerciseLoader.loadExercisesForUserPrefs(context, goal, level, location, timePref);
        totalExercisesToday = todayExercises.size();

        for (int i = 0; i < todayExercises.size(); i++) {
            Exercise ex = todayExercises.get(i);
            final int index = i;

            LinearLayout cardLayout = createExerciseCard(context, ex);
            workoutListContainer.addView(cardLayout);

            if (i == 0) cardLayout.setBackgroundResource(R.drawable.exercise_card_active_bg);

            LinearLayout btnLayout = (LinearLayout) cardLayout.getChildAt(2);
            Button startBtn = (Button) btnLayout.getChildAt(0);
            Button pauseBtn = (Button) btnLayout.getChildAt(1);
            Button stopBtn = (Button) btnLayout.getChildAt(2);
            TextView timerTv = (TextView) ((LinearLayout) cardLayout.getChildAt(1)).getChildAt(2);
            ImageView checkMark = (ImageView) btnLayout.getChildAt(3);

            final CountDownTimer[] timer = new CountDownTimer[1];

            startBtn.setOnClickListener(v -> {
                startBtn.setEnabled(false);
                pauseBtn.setEnabled(true);
                stopBtn.setEnabled(true);

                timer[0] = new CountDownTimer(ex.durationSec * 1000, 1000) {
                    long remaining = ex.durationSec;

                    @Override
                    public void onTick(long millisUntilFinished) {
                        remaining = millisUntilFinished / 1000;
                        timerTv.setText(String.format("%02d:%02d", remaining / 60, remaining % 60));
                    }

                    @Override
                    public void onFinish() {
                        timerTv.setText("Done");
                        pauseBtn.setEnabled(false);
                        stopBtn.setEnabled(false);
                        checkMark.setImageResource(android.R.drawable.checkbox_on_background);

                        cardLayout.animate().alpha(0.5f).setDuration(500).start();

                        historyManager.addWorkout(ex);
                        completedExercisesToday++;
                        updateDailyProgress();

                        if (index + 1 < totalExercisesToday) {
                            LinearLayout nextCard = (LinearLayout) workoutListContainer.getChildAt(index + 1);
                            nextCard.setBackgroundResource(R.drawable.exercise_card_active_bg);
                            startRestTimer(index);
                        }
                    }
                }.start();
            });

            pauseBtn.setOnClickListener(v -> {
                if (timer[0] != null) {
                    timer[0].cancel();
                    startBtn.setEnabled(true);
                    pauseBtn.setEnabled(false);
                }
            });

            stopBtn.setOnClickListener(v -> {
                if (timer[0] != null) {
                    timer[0].cancel();
                    timerTv.setText("00:00");
                    startBtn.setEnabled(true);
                    pauseBtn.setEnabled(false);
                    stopBtn.setEnabled(false);
                }
            });
        }
    }

    private LinearLayout createExerciseCard(Context context, Exercise ex) {
        LinearLayout cardLayout = new LinearLayout(context);
        cardLayout.setOrientation(LinearLayout.HORIZONTAL);
        cardLayout.setPadding(20, 20, 20, 20);
        cardLayout.setBackgroundResource(R.drawable.exercise_card_bg);
        cardLayout.setElevation(8f);

        ImageView img = new ImageView(context);
        img.setImageResource(ex.imageRes);
        img.setLayoutParams(new LinearLayout.LayoutParams(200, 200));
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);

        LinearLayout textLayout = new LinearLayout(context);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.setPadding(24, 0, 24, 0);
        textLayout.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView tvName = new TextView(context);
        tvName.setText(ex.name);
        tvName.setTypeface(Typeface.DEFAULT_BOLD);
        tvName.setTextSize(18f);

        TextView tvSets = new TextView(context);
        tvSets.setText(ex.setsReps);
        tvSets.setTextSize(14f);

        TextView tvTimer = new TextView(context);
        tvTimer.setText("00:00");
        tvTimer.setTextSize(14f);
        tvTimer.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));

        textLayout.addView(tvName);
        textLayout.addView(tvSets);
        textLayout.addView(tvTimer);

        LinearLayout btnLayout = new LinearLayout(context);
        btnLayout.setOrientation(LinearLayout.VERTICAL);
        btnLayout.setGravity(Gravity.CENTER_VERTICAL);

        Button startBtn = new Button(context);
        startBtn.setText("Start");
        startBtn.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
        startBtn.setTextColor(getResources().getColor(android.R.color.white));

        Button pauseBtn = new Button(context);
        pauseBtn.setText("Pause");
        pauseBtn.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light));
        pauseBtn.setTextColor(getResources().getColor(android.R.color.white));
        pauseBtn.setEnabled(false);

        Button stopBtn = new Button(context);
        stopBtn.setText("Stop");
        stopBtn.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
        stopBtn.setTextColor(getResources().getColor(android.R.color.white));
        stopBtn.setEnabled(false);

        ImageView checkMark = new ImageView(context);
        checkMark.setImageResource(android.R.drawable.checkbox_off_background);
        LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(60, 60);
        checkParams.setMargins(8, 0, 0, 0);
        checkMark.setLayoutParams(checkParams);

        btnLayout.addView(startBtn);
        btnLayout.addView(pauseBtn);
        btnLayout.addView(stopBtn);
        btnLayout.addView(checkMark);

        cardLayout.addView(img);
        cardLayout.addView(textLayout);
        cardLayout.addView(btnLayout);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 24);
        cardLayout.setLayoutParams(cardParams);

        return cardLayout;
    }

    private void startRestTimer(int nextIndex) {
        if (nextIndex + 1 >= totalExercisesToday) return;

        LinearLayout nextCard = (LinearLayout) workoutListContainer.getChildAt(nextIndex + 1);
        TextView timerTv = (TextView) ((LinearLayout) nextCard.getChildAt(1)).getChildAt(2);
        timerTv.setText("Rest: " + REST_SECONDS + "s");

        new CountDownTimer(REST_SECONDS * 1000, 1000) {
            int remaining = REST_SECONDS;

            @Override
            public void onTick(long millisUntilFinished) {
                remaining--;
                timerTv.setText("Rest: " + remaining + "s");
            }

            @Override
            public void onFinish() {
                timerTv.setText("00:00");
            }
        }.start();
    }

    private void updateDailyProgress() {
        int progress = totalExercisesToday == 0 ? 0 : (int) ((completedExercisesToday * 100.0f) / totalExercisesToday);
        dailyProgressBar.setProgress(progress);
        progressText.setText(progress + "% completed");
    }
}
