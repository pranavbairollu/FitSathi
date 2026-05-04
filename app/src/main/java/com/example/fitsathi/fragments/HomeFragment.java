package com.example.fitsathi.fragments;

import android.animation.ObjectAnimator;
import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.fitsathi.DashboardActivity;
import com.example.fitsathi.FitnessGoalActivity;
import com.example.fitsathi.R;
import com.example.fitsathi.WorkoutActivity;
import com.example.fitsathi.managers.WaterIntakeManager;
import com.example.fitsathi.utils.DateUtils;
import com.example.fitsathi.models.Exercise;
import com.example.fitsathi.services.StepCounterService;
import com.example.fitsathi.data.ExerciseLoader;
import com.google.android.material.card.MaterialCardView;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import nl.dionsegijn.konfetti.KonfettiView;
import nl.dionsegijn.konfetti.models.Shape;
import nl.dionsegijn.konfetti.models.Size;

public class HomeFragment extends Fragment {

    private CircularProgressBar circularProgressBar;
    private TextView stepsText, caloriesText, distanceText, changeGoalButton, homeWaterCountText;
    private int currentSteps = 0;
    private int stepGoal = 10000;
    private SharedPreferences stepPrefs, fitnessPrefs, waterPrefs;
    private LinearLayout todayWorkoutContainer;
    private RelativeLayout todayWorkoutHeader;
    private Button startWorkoutButton;
    private List<Exercise> todayExercises;
    private KonfettiView konfettiView;
    private MaterialCardView quickAccessTracker, quickAccessBarChart, quickAccessStepsChart, quickAccessProfile, quickAccessWater;
    private StepCounterService mService;
    private boolean mBound = false;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startStepCounterService();
                } else {
                    Toast.makeText(getContext(), "Permission denied. Step counter will not work.", Toast.LENGTH_SHORT).show();
                }
            });

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            StepCounterService.StepCounterBinder binder = (StepCounterService.StepCounterBinder) service;
            mService = binder.getService();
            mBound = true;
            updateStepsFromService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    private final BroadcastReceiver stepUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (StepCounterService.ACTION_STEPS_UPDATED.equals(intent.getAction())) {
                currentSteps = intent.getIntExtra("steps", 0);
                updateStepDisplay();
            }
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        if (getContext() != null) {
            androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(requireContext())
                    .registerReceiver(stepUpdateReceiver, new android.content.IntentFilter(StepCounterService.ACTION_STEPS_UPDATED));
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (getContext() != null) {
            androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(requireContext())
                    .unregisterReceiver(stepUpdateReceiver);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getContext() != null) {
            Intent intent = new Intent(requireContext(), StepCounterService.class);
            requireContext().bindService(intent, connection, Context.BIND_AUTO_CREATE);
        }
        updateWaterDisplay();
        updateStepsFromService();
        loadUserInfoAndRefresh();
    }

    private void loadUserInfoAndRefresh() {
        com.example.fitsathi.managers.UserInfoManager.getUserInfo(userInfo -> {
            if (isAdded() && userInfo != null) {
                mUserInfo = userInfo;
                requireActivity().runOnUiThread(() -> updateStepDisplay());
            }
        });
    }

    private com.example.fitsathi.managers.UserInfoManager.UserInfo mUserInfo;

    @Override
    public void onPause() {
        super.onPause();
        if (getContext() != null && mBound) {
            requireContext().unbindService(connection);
            mBound = false;
        }
    }

    private void updateStepsFromService() {
        if (mBound && mService != null) {
            currentSteps = mService.getSteps();
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> updateStepDisplay());
            }
        } else {
            // Fallback: Read from SharedPreferences if service not bound yet
            currentSteps = stepPrefs.getInt("daily_steps", 0);
            if (isAdded()) {
                updateStepDisplay();
            }
        }
    }

    private static int getThemeColor(Context context, int attr) {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        context.getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        circularProgressBar = view.findViewById(R.id.circularProgressBar);
        stepsText = view.findViewById(R.id.steps_text);
        caloriesText = view.findViewById(R.id.calories_text);
        distanceText = view.findViewById(R.id.distance_text);
        changeGoalButton = view.findViewById(R.id.change_goal_button);
        todayWorkoutContainer = view.findViewById(R.id.today_workout_container);
        todayWorkoutHeader = view.findViewById(R.id.today_workout_header);
        startWorkoutButton = view.findViewById(R.id.btn_start_workout);
        quickAccessTracker = view.findViewById(R.id.open_tracker);
        quickAccessBarChart = view.findViewById(R.id.open_barchart);
        quickAccessStepsChart = view.findViewById(R.id.open_steps_chart);
        quickAccessProfile = view.findViewById(R.id.open_profile);
        quickAccessWater = view.findViewById(R.id.open_water_tracker);
        homeWaterCountText = view.findViewById(R.id.tv_home_water_count);
        konfettiView = view.findViewById(R.id.konfettiView);

        Context context = requireContext();
        // Unify SharedPreferences to StepCounterPrefs
        stepPrefs = context.getSharedPreferences("StepCounterPrefs", Context.MODE_PRIVATE);
        fitnessPrefs = context.getSharedPreferences("FitnessPrefs", Context.MODE_PRIVATE);
        waterPrefs = context.getSharedPreferences("WaterPrefs", Context.MODE_PRIVATE);

        // Check for legacy goal in StepPrefs and migrate if needed
        if (!stepPrefs.contains("daily_goal")) {
            SharedPreferences legacyPrefs = context.getSharedPreferences("StepPrefs", Context.MODE_PRIVATE);
            int legacyGoal = legacyPrefs.getInt("daily_goal", 10000);
            stepPrefs.edit().putInt("daily_goal", legacyGoal).apply();
        }
        
        stepGoal = stepPrefs.getInt("daily_goal", 10000);

        initCircularProgressBar();
        initQuickAccessCards();
        initStepCounter();
        startStepCounterService();

        changeGoalButton.setOnClickListener(v -> openGoalActivity());
        startWorkoutButton.setOnClickListener(v -> launchWorkoutActivity());
        circularProgressBar.setOnClickListener(v -> showChangeGoalDialog());

        loadTodayWorkout();

        return view;
    }

    private void showChangeGoalDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_change_goal, null);
        builder.setView(dialogView);

        EditText newGoalEditText = dialogView.findViewById(R.id.goal_input);
        Button saveGoalButton = dialogView.findViewById(R.id.set_goal_button);

        AlertDialog dialog = builder.create();

        saveGoalButton.setOnClickListener(v -> {
            String newGoalStr = newGoalEditText.getText().toString();
            if (!TextUtils.isEmpty(newGoalStr)) {
                try {
                    int newGoal = Integer.parseInt(newGoalStr);
                    stepPrefs.edit().putInt("daily_goal", newGoal).apply();
                    stepGoal = newGoal;
                    updateStepDisplay();
                    initCircularProgressBar();
                    Toast.makeText(getContext(), "Step goal updated to " + newGoal, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "Please enter a valid number", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), "Please enter a valid goal", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void initCircularProgressBar() {
        circularProgressBar.setProgress(0f);
        circularProgressBar.setProgressMax(stepGoal);
        circularProgressBar.setProgressBarColor(getThemeColor(requireContext(), com.google.android.material.R.attr.colorPrimary));
        circularProgressBar.setBackgroundProgressBarColor(getThemeColor(requireContext(), com.google.android.material.R.attr.colorSurface));
        circularProgressBar.setProgressBarWidth(12f);
        circularProgressBar.setBackgroundProgressBarWidth(8f);
        circularProgressBar.setRoundBorder(true);
        circularProgressBar.setStartAngle(270f);
    }

    private void initQuickAccessCards() {
        View.OnClickListener listener = v -> {
            if (getActivity() instanceof DashboardActivity) {
                DashboardActivity activity = (DashboardActivity) getActivity();
                int id = v.getId();
                if (id == R.id.open_tracker) {
                    activity.openTrackerFragment();
                } else if (id == R.id.open_barchart) {
                    activity.openBarChartFragment();
                } else if (id == R.id.open_steps_chart) {
                    activity.openStepsChartFragment();
                } else if (id == R.id.open_profile) {
                    activity.openProfileFragment();
                } else if (id == R.id.open_water_tracker) {
                    activity.openWaterFragment();
                }
            }
        };

        quickAccessTracker.setOnClickListener(listener);
        quickAccessBarChart.setOnClickListener(listener);
        quickAccessStepsChart.setOnClickListener(listener);
        quickAccessProfile.setOnClickListener(listener);
        quickAccessWater.setOnClickListener(listener);
    }

    private void initStepCounter() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION);
        }
    }

    private void startStepCounterService() {
        Intent serviceIntent = new Intent(requireContext(), StepCounterService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(serviceIntent);
        } else {
            requireContext().startService(serviceIntent);
        }
    }

    private void updateStepDisplay() {
        if (stepsText == null || circularProgressBar == null) return;
        
        // Handle "No Sensor" or initial state
        if (currentSteps < 0) {
            stepsText.setText("Tracker loading...");
            return;
        }

        stepsText.setText(String.format(Locale.getDefault(), "%d / %d Steps", currentSteps, stepGoal));
        float progress = Math.min(currentSteps, stepGoal);
        ObjectAnimator.ofFloat(circularProgressBar, "progress", circularProgressBar.getProgress(), progress)
                .setDuration(400)
                .start();

        // --- PRECISION CALCULATIONS ---
        float calories;
        float distance;

        if (mUserInfo != null && mUserInfo.getHeight() > 0 && mUserInfo.getWeight() > 0) {
            // Formula: Stride length (m) = height (cm) * 0.415 / 100
            float strideLength = (mUserInfo.getHeight() * 0.415f) / 100f;
            distance = (currentSteps * strideLength) / 1000f; // in km
            
            // Formula: Calories = steps * 0.04 * (weight / 70)
            calories = currentSteps * 0.04f * (mUserInfo.getWeight() / 70f);
        } else {
            // Default fallbacks
            calories = currentSteps * 0.04f;
            distance = currentSteps * 0.0008f;
        }

        caloriesText.setText(String.format(Locale.getDefault(), "🔥 %d Kcal", Math.round(calories)));
        distanceText.setText(String.format(Locale.US, "📍 %.2f km", distance));

        boolean goalReached = currentSteps >= stepGoal;
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String lastCelebratedDate = stepPrefs.getString("last_celebrated_date", "");
        int lastCelebratedGoal = stepPrefs.getInt("last_celebrated_goal", 0);

        if (goalReached) {
            if (!todayDate.equals(lastCelebratedDate) || stepGoal > lastCelebratedGoal) {
                showKonfetti();
                stepPrefs.edit()
                        .putString("last_celebrated_date", todayDate)
                        .putInt("last_celebrated_goal", stepGoal)
                        .apply();
            }
        }
    }

    private void updateWaterDisplay() {
        if (homeWaterCountText == null) return;
        String todayDate = DateUtils.getFoodLogDate();
        int waterGoal = WaterIntakeManager.getWaterGoal(requireContext());
        
        WaterIntakeManager.getWaterLog(requireContext(), todayDate, log -> {
            if (isAdded() && homeWaterCountText != null) {
                homeWaterCountText.setText(String.format(Locale.getDefault(), "%d/%d", log.size(), waterGoal));
            }
        });
    }

    private void showKonfetti() {
        konfettiView.setVisibility(View.VISIBLE);
        konfettiView.build()
                .addColors(
                        getThemeColor(requireContext(), com.google.android.material.R.attr.colorPrimary),
                        getThemeColor(requireContext(), com.google.android.material.R.attr.colorSecondary),
                        ContextCompat.getColor(requireContext(), R.color.brand_accent),
                        getThemeColor(requireContext(), com.google.android.material.R.attr.colorPrimary)
                )
                .setDirection(0.0, 359.0)
                .setSpeed(4f, 7f)
                .setFadeOutEnabled(true)
                .setTimeToLive(2000L)
                .addShapes(Shape.Square.INSTANCE, Shape.Circle.INSTANCE)
                .addSizes(new Size(12, 5f))
                .setPosition(-50f, konfettiView.getWidth() + 50f, -50f, -50f)
                .burst(300);
    }

    private void openGoalActivity() {
        startActivity(new Intent(requireContext(), FitnessGoalActivity.class));
    }

    private void launchWorkoutActivity() {
        Intent intent = new Intent(requireContext(), WorkoutActivity.class);
        startActivity(intent);
    }

    private void loadTodayWorkout() {
        // Get context early
        Context context = getContext();
        if (context == null) return; // Fragment not attached, abort

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            // Read user preferences in background
            String goal = fitnessPrefs.getString("goal", "");
            String level = fitnessPrefs.getString("level", "");
            String location = fitnessPrefs.getString("location", "");
            String timePref = fitnessPrefs.getString("time", "");

            List<Exercise> exercises;
            try {
                exercises = ExerciseLoader.loadExercisesForUserPrefs(context, goal, level, location, timePref);
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.e("HomeFragment", "Error loading exercises, likely an issue in ExerciseLoader.", e);
                exercises = Collections.emptyList();
            }

            // Save to class-level field so it can be used elsewhere safely
            todayExercises = exercises;

            // Update UI on main thread
            handler.post(() -> {
                if (!isAdded()) return; // Fragment detached, abort UI update
                if (todayWorkoutContainer == null || todayWorkoutHeader == null) return;

                todayWorkoutContainer.removeAllViews();

                if (todayExercises == null || todayExercises.isEmpty()) {
                    todayWorkoutHeader.setVisibility(View.GONE);
                    todayWorkoutContainer.setVisibility(View.GONE);
                    return;
                }

                todayWorkoutHeader.setVisibility(View.VISIBLE);
                todayWorkoutContainer.setVisibility(View.VISIBLE);

                for (Exercise ex : todayExercises) {
                    if (ex == null) continue;

                    View card = LayoutInflater.from(context)
                            .inflate(R.layout.item_today_workout, todayWorkoutContainer, false);

                    ImageView img = card.findViewById(R.id.img_exercise);
                    TextView nameTv = card.findViewById(R.id.tv_exercise_name);
                    TextView setsTv = card.findViewById(R.id.tv_exercise_sets);
                    TextView durationTv = card.findViewById(R.id.tv_exercise_duration);
                    Button startBtn = card.findViewById(R.id.btn_start_exercise);

                    img.setImageResource(ex.imageRes);
                    nameTv.setText(ex.name);

                    if (ex.setsReps != null && !ex.setsReps.isEmpty()) {
                        setsTv.setText(String.format(Locale.getDefault(), "%s reps", ex.setsReps.replace("x", " sets x ")));
                    } else {
                        setsTv.setVisibility(View.GONE);
                    }

                    if (ex.duration > 0) {
                        durationTv.setText(String.format(Locale.getDefault(), "%d mins", ex.duration));
                    } else {
                        durationTv.setVisibility(View.GONE);
                    }

                    startBtn.setOnClickListener(v -> launchWorkoutActivity());

                    todayWorkoutContainer.addView(card);
                }
            });
        });
    }
}
