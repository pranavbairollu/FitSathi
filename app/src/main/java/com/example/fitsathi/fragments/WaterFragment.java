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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WaterFragment extends Fragment {

    private TextView waterCountText;
    private FloatingActionButton increaseButton;
    private MaterialButton decreaseButton;
    private RecyclerView waterLogRecyclerView;
    private WaterLogAdapter waterLogAdapter;

    private List<Long> waterLog;
    private final int waterGoal = 8;
    private SharedPreferences waterPrefs;
    private String todayDate;
    private final Gson gson = new Gson();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_water, container, false);

        waterCountText = view.findViewById(R.id.tv_water_count);
        increaseButton = view.findViewById(R.id.fab_increase_water);
        decreaseButton = view.findViewById(R.id.btn_decrease_water);
        waterLogRecyclerView = view.findViewById(R.id.rv_water_log);

        waterPrefs = requireActivity().getSharedPreferences("WaterPrefs", Context.MODE_PRIVATE);
        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        loadWaterLog();
        setupRecyclerView();

        increaseButton.setOnClickListener(v -> addWaterEntry());
        decreaseButton.setOnClickListener(v -> removeWaterEntry());

        updateUI();

        return view;
    }

    private void loadWaterLog() {
        String json = waterPrefs.getString(todayDate + "_log", null);
        if (json != null) {
            Type type = new TypeToken<ArrayList<Long>>() {}.getType();
            waterLog = gson.fromJson(json, type);
        } else {
            waterLog = new ArrayList<>();
        }
    }

    private void saveWaterLog() {
        SharedPreferences.Editor editor = waterPrefs.edit();
        editor.putInt(todayDate, waterLog.size());
        String json = gson.toJson(waterLog);
        editor.putString(todayDate + "_log", json);
        editor.apply();
    }

    private void setupRecyclerView() {
        waterLogAdapter = new WaterLogAdapter(waterLog);
        waterLogRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        waterLogRecyclerView.setAdapter(waterLogAdapter);
    }

    private void addWaterEntry() {
        if (waterLog.size() < 99) {
            waterLog.add(System.currentTimeMillis());
            saveWaterLog();
            updateUI();
            waterLogAdapter.updateLog(waterLog);
        }
    }

    private void removeWaterEntry() {
        if (!waterLog.isEmpty()) {
            waterLog.remove(waterLog.size() - 1);
            saveWaterLog();
            updateUI();
            waterLogAdapter.updateLog(waterLog);
        }
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
