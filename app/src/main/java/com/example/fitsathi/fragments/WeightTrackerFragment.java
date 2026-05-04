package com.example.fitsathi.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.fitsathi.databinding.FragmentWeightTrackerBinding;
import com.example.fitsathi.R;
import com.example.fitsathi.managers.WeightLogManager;
import com.example.fitsathi.models.WeightLog;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class WeightTrackerFragment extends Fragment implements WeightLogManager.WeightLogCallback {

    private FragmentWeightTrackerBinding binding;
    private int currentDays = 7;
    private static final String PREFS_NAME = "WeightPrefs";
    private static final String TARGET_WEIGHT_KEY = "targetWeight";
    private static final String TAG = "WeightTrackerFragment";
    private DatabaseReference targetWeightRef;
    private ValueEventListener targetWeightListener;
    private float currentTargetWeight = -1f;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentWeightTrackerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupChart();
        setupClickListeners();
        binding.toggleButtonGroup.check(R.id.week_button);
        updateChart(currentDays);
        setupTargetWeightListener();
    }

    private void setupClickListeners() {
        binding.logWeightButton.setOnClickListener(v -> {
            if (binding.weightInput.getText() != null) {
                String input = binding.weightInput.getText().toString().trim();
                if (!input.isEmpty()) {
                    try {
                        float weight = Float.parseFloat(input);
                        if (weight >= 20 && weight <= 300) { // Input validation
                            WeightLogManager.saveWeight(requireContext(), weight);
                            Toast.makeText(requireContext(), R.string.weight_saved, Toast.LENGTH_SHORT).show();
                            updateChart(currentDays);
                            binding.weightInput.setText("");
                        } else {
                            Toast.makeText(requireContext(), "Please enter a weight between 20kg and 300kg", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(requireContext(), R.string.invalid_weight, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), R.string.please_enter_weight, Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.setTargetWeightButton.setOnClickListener(v -> {
            if (binding.targetWeightInput.getText() != null) {
                String input = binding.targetWeightInput.getText().toString().trim();
                if (!input.isEmpty()) {
                    try {
                        float targetWeight = Float.parseFloat(input);
                         if (targetWeight >= 20 && targetWeight <= 300) { // Input validation
                            saveTargetWeight(targetWeight);
                            Toast.makeText(requireContext(), R.string.target_weight_saved, Toast.LENGTH_SHORT).show();
                            binding.targetWeightInput.setText("");
                        } else {
                            Toast.makeText(requireContext(), "Please enter a target weight between 20kg and 300kg", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(requireContext(), R.string.invalid_weight, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(), R.string.please_enter_target_weight, Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.toggleButtonGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.week_button) {
                    currentDays = 7;
                } else if (checkedId == R.id.month_button) {
                    currentDays = 30;
                } else if (checkedId == R.id.all_button) {
                    currentDays = 365; // Or a large number to signify all
                }
                updateChart(currentDays);
            }
        });
    }

    private void setupChart() {
        binding.weightChart.getDescription().setEnabled(false);
        binding.weightChart.getLegend().setEnabled(false);
        binding.weightChart.setTouchEnabled(true);
        binding.weightChart.setDragEnabled(true);
        binding.weightChart.setScaleEnabled(true);
        binding.weightChart.setPinchZoom(true);
        binding.weightChart.setExtraBottomOffset(10f);

        XAxis xAxis = binding.weightChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(getThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant));

        YAxis leftAxis = binding.weightChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGranularity(1f);
        leftAxis.setTextColor(getThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant));

        binding.weightChart.getAxisRight().setEnabled(false);
    }

    private void updateChart(int days) {
        WeightLogManager.getLastNWeightEntries(requireContext(), days, this);
    }

    @Override
    public void onLogsReceived(List<WeightLog> weightLogs) {
        if (isAdded() && binding != null) {
            if (weightLogs == null || weightLogs.isEmpty()) {
                binding.weightChart.setVisibility(View.GONE);
                binding.emptyChartText.setVisibility(View.VISIBLE);
                binding.currentWeightText.setText(R.string.weight_placeholder);
                binding.weightChangeText.setText(R.string.weight_placeholder);
                return;
            }

            binding.weightChart.setVisibility(View.VISIBLE);
            binding.emptyChartText.setVisibility(View.GONE);

            ArrayList<Entry> entries = new ArrayList<>();
            final ArrayList<Long> timestamps = new ArrayList<>();

            for (int i = 0; i < weightLogs.size(); i++) {
                WeightLog log = weightLogs.get(i);
                entries.add(new Entry(i, log.getWeight()));
                timestamps.add(log.getTimestamp());
            }

            XAxis xAxis = binding.weightChart.getXAxis();
            if (currentDays == 30 || currentDays > 30) {
                 xAxis.setLabelCount(5, true);
            } else {
                 xAxis.setLabelCount(weightLogs.size(), false);
            }
            xAxis.setValueFormatter(new ValueFormatter() {
                private final SimpleDateFormat sdf = new SimpleDateFormat(currentDays > 7 ? "MMM dd" : "EEE", Locale.getDefault());
                @Override
                public String getFormattedValue(float value) {
                    int index = (int) value;
                    if (index >= 0 && index < timestamps.size()) {
                        return sdf.format(new Date(timestamps.get(index)));
                    }
                    return "";
                }
            });

            float currentWeight = weightLogs.get(weightLogs.size() - 1).getWeight();
            binding.currentWeightText.setText(String.format(Locale.getDefault(), "%.1f kg", currentWeight));

            if (weightLogs.size() > 1) {
                float firstWeight = weightLogs.get(0).getWeight();
                float change = currentWeight - firstWeight;
                binding.weightChangeText.setText(String.format(Locale.getDefault(), "%+.1f kg", change));
                 int changeColor;
                if (change < 0) {
                    changeColor = ContextCompat.getColor(requireContext(), R.color.brand_secondary);
                } else if (change > 0) {
                    changeColor = getThemeColor(com.google.android.material.R.attr.colorError);
                } else {
                    changeColor = getThemeColor(com.google.android.material.R.attr.colorOnSurface);
                }
                binding.weightChangeText.setTextColor(changeColor);
            } else {
                binding.weightChangeText.setText("No change");
                 binding.weightChangeText.setTextColor(getThemeColor(com.google.android.material.R.attr.colorOnSurface));
            }


            LineDataSet dataSet = new LineDataSet(entries, getString(R.string.weight_label));
            dataSet.setLineWidth(2.5f);
            dataSet.setCircleRadius(4.5f);
            dataSet.setCircleHoleRadius(2f);
            dataSet.setColor(getThemeColor(com.google.android.material.R.attr.colorPrimary));
            dataSet.setCircleColor(getThemeColor(com.google.android.material.R.attr.colorPrimary));
            dataSet.setHighLightColor(getThemeColor(com.google.android.material.R.attr.colorSecondary));
            dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            dataSet.setDrawFilled(true);
            dataSet.setDrawValues(false);

            GradientDrawable gd = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{getThemeColor(com.google.android.material.R.attr.colorPrimaryContainer),
                            ContextCompat.getColor(requireContext(), android.R.color.transparent)}
            );
            dataSet.setFillDrawable(gd);

            LineData lineData = new LineData(dataSet);
            binding.weightChart.setData(lineData);

            if (currentTargetWeight > 0) {
                float minWeight = Float.MAX_VALUE;
                float maxWeight = Float.MIN_VALUE;
                for (WeightLog log : weightLogs) {
                    minWeight = Math.min(minWeight, log.getWeight());
                    maxWeight = Math.max(maxWeight, log.getWeight());
                }
                minWeight = Math.min(minWeight, currentTargetWeight);
                maxWeight = Math.max(maxWeight, currentTargetWeight);
                
                YAxis leftAxis = binding.weightChart.getAxisLeft();
                leftAxis.setAxisMinimum(minWeight - 5f);
                leftAxis.setAxisMaximum(maxWeight + 5f);
            }

            updateLimitLine();

            binding.weightChart.animateX(500);
            binding.weightChart.invalidate();
        }
    }


    @Override
    public void onError(String message) {
        if(isAdded()) {
            Toast.makeText(requireContext(), "Error loading weight data.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error loading weight data: " + message);
        }
    }

    private int getThemeColor(int attr) {
        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    private void saveTargetWeight(float weight) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("users").child(userId).child("target_weight");
            databaseReference.setValue(weight);
        }

        // Cache locally
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putFloat(TARGET_WEIGHT_KEY, weight).apply();
        currentTargetWeight = weight;
        updateLimitLine();
    }

    private void setupTargetWeightListener() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            targetWeightRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("target_weight");

            targetWeightListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Float targetWeight = dataSnapshot.getValue(Float.class);
                    if (targetWeight != null) {
                        currentTargetWeight = targetWeight;
                        binding.targetWeightText.setText(String.format(Locale.getDefault(), "%.1f kg", targetWeight));
                    } else {
                        // Try loading from cache if Firebase returns no value
                        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                        float cachedWeight = prefs.getFloat(TARGET_WEIGHT_KEY, -1f);
                        if (cachedWeight != -1f) {
                            currentTargetWeight = cachedWeight;
                            binding.targetWeightText.setText(String.format(Locale.getDefault(), "%.1f kg", cachedWeight));
                        } else {
                            binding.targetWeightText.setText(R.string.weight_placeholder);
                        }
                    }
                    updateLimitLine();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "Error loading target weight: " + databaseError.getMessage());
                    // Try loading from cache on error
                    SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    float cachedWeight = prefs.getFloat(TARGET_WEIGHT_KEY, -1f);
                    if (cachedWeight != -1f) {
                        currentTargetWeight = cachedWeight;
                        binding.targetWeightText.setText(String.format(Locale.getDefault(), "%.1f kg", cachedWeight));
                    } else {
                        binding.targetWeightText.setText(R.string.weight_placeholder);
                    }
                    updateLimitLine();
                }
            };
            targetWeightRef.addValueEventListener(targetWeightListener);
        }    
    }

    private void updateLimitLine() {
        if (binding == null || binding.weightChart == null || currentTargetWeight <= 0) return;
        YAxis leftAxis = binding.weightChart.getAxisLeft();
        leftAxis.removeAllLimitLines();
        
        com.github.mikephil.charting.components.LimitLine targetLine = new com.github.mikephil.charting.components.LimitLine(currentTargetWeight, "Target: " + currentTargetWeight + " kg");
        targetLine.setLineWidth(2f);
        targetLine.setLineColor(getThemeColor(com.google.android.material.R.attr.colorPrimary));
        targetLine.setTextColor(getThemeColor(com.google.android.material.R.attr.colorOnSurface));
        targetLine.enableDashedLine(10f, 10f, 0f);
        targetLine.setLabelPosition(com.github.mikephil.charting.components.LimitLine.LimitLabelPosition.RIGHT_TOP);
        
        leftAxis.addLimitLine(targetLine);
        binding.weightChart.invalidate();
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (targetWeightRef != null && targetWeightListener != null) {
            targetWeightRef.removeEventListener(targetWeightListener);
        }
        binding = null;
    }
}
