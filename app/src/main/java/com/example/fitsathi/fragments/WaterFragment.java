package com.example.fitsathi.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitsathi.R;
import com.example.fitsathi.adapters.WaterLogAdapter;
import com.example.fitsathi.managers.WaterIntakeManager;
import com.example.fitsathi.utils.DateUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.widget.EditText;
import android.text.InputType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WaterFragment extends Fragment {

    private TextView waterCountText;
    private FloatingActionButton increaseButton;
    private MaterialButton decreaseButton;
    private RecyclerView waterLogRecyclerView;
    private WaterLogAdapter waterLogAdapter;

    private List<Long> waterLog = new ArrayList<>();
    private int waterGoal;
    private String todayDate;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_water, container, false);

        waterCountText = view.findViewById(R.id.tv_water_count);
        increaseButton = view.findViewById(R.id.fab_increase_water);
        decreaseButton = view.findViewById(R.id.btn_decrease_water);
        waterLogRecyclerView = view.findViewById(R.id.rv_water_log);

        todayDate = DateUtils.getFoodLogDate();
        waterGoal = WaterIntakeManager.getWaterGoal(requireContext());

        setupRecyclerView();
        loadWaterData();

        increaseButton.setOnClickListener(v -> addWaterEntry());
        decreaseButton.setOnClickListener(v -> removeWaterEntry());
        
        // Long click on count text to change goal
        waterCountText.setOnLongClickListener(v -> {
            showChangeGoalDialog();
            return true;
        });

        updateUI();

        return view;
    }

    private void loadWaterData() {
        WaterIntakeManager.getWaterLog(requireContext(), todayDate, log -> {
            if (isAdded()) {
                waterLog = log;
                waterLogAdapter.updateLog(waterLog);
                updateUI();
            }
        });
    }

    private void setupRecyclerView() {
        waterLogAdapter = new WaterLogAdapter(waterLog);
        waterLogRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        waterLogRecyclerView.setAdapter(waterLogAdapter);
    }

    private void addWaterEntry() {
        if (waterLog.size() < 99) {
            WaterIntakeManager.addWaterEntry(requireContext(), todayDate, success -> {
                if (success) loadWaterData();
            });
        }
    }

    private void removeWaterEntry() {
        if (!waterLog.isEmpty()) {
            WaterIntakeManager.removeWaterEntry(requireContext(), todayDate, success -> {
                if (success) loadWaterData();
            });
        }
    }

    private void showChangeGoalDialog() {
        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(waterGoal));

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Set Daily Goal")
                .setMessage("Enter your target water intake (glasses):")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String val = input.getText().toString();
                    if (!val.isEmpty()) {
                        int newGoal = Integer.parseInt(val);
                        WaterIntakeManager.setWaterGoal(requireContext(), newGoal);
                        waterGoal = newGoal;
                        updateUI();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateUI() {
        if (waterCountText != null) {
            waterCountText.setText(String.format(Locale.getDefault(), "%d / %d", waterLog.size(), waterGoal));
        }
        if (decreaseButton != null) {
            decreaseButton.setEnabled(!waterLog.isEmpty());
        }
    }
}
