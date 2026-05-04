package com.example.fitsathi.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.google.android.material.button.MaterialButtonToggleGroup;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.fitsathi.R;
import com.example.fitsathi.managers.StepCounterManager;
import com.example.fitsathi.utils.CustomMarkerView;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class StepsChartFragment extends Fragment {

    private BarChart barChart;
    private ProgressBar progressBar;
    private TextView emptyTextView;
    private MaterialButtonToggleGroup toggleButtonGroup;
    private int currentDays = 7;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_steps_chart, container, false);
        barChart = view.findViewById(R.id.steps_bar_chart);
        progressBar = view.findViewById(R.id.chart_progress_bar);
        emptyTextView = view.findViewById(R.id.empty_chart_text);
        toggleButtonGroup = view.findViewById(R.id.toggleButtonGroup);

        setupChart();
        
        toggleButtonGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.week_button) {
                    currentDays = 7;
                } else if (checkedId == R.id.month_button) {
                    currentDays = 30;
                }
                updateChartData();
            }
        });
        
        return view;
    }

    private void setupChart() {
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setExtraOffsets(0, 0, 0, 10f);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(getThemeColor(com.google.android.material.R.attr.colorOnSurface));
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setTextColor(getThemeColor(com.google.android.material.R.attr.colorOnSurface));
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(getThemeColor(com.google.android.material.R.attr.colorOutlineVariant));
        leftAxis.setAxisMinimum(0f);
        leftAxis.setSpaceTop(30f);

        barChart.getAxisRight().setEnabled(false);

        barChart.setHighlightPerTapEnabled(true);
        barChart.setDrawMarkers(true);

        if (getContext() != null) {
            try {
                CustomMarkerView markerView = new CustomMarkerView(requireContext(), R.layout.custom_marker_view);
                markerView.setChartView(barChart);
                barChart.setMarker(markerView);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void updateChartData() {
        if (!isAdded()) return;
        showLoadingState(true);

        // Hybrid data loading: Fetch historical data from Firebase
        StepCounterManager.getLastNDaysSteps(currentDays, stepsMap -> {
            final View view = getView();
            if (!isAdded() || view == null || stepsMap == null) {
                if (view != null) {
                    view.post(() -> {
                        showLoadingState(false);
                        showEmptyState(true);
                    });
                }
                return;
            }

            // Get today's live step count asynchronously
            StepCounterManager.getSteps(new StepCounterManager.StepCountCallback() {
                @Override
                public void onStepCountReceived(int todaysSteps) {
                    // This is the callback. All logic depending on today's steps now lives here.
                    String todayDateKey = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());

                    // Use a TreeMap to keep the map sorted by date
                    Map<String, Integer> sortedStepsMap = new TreeMap<>(stepsMap);

                    // Update today's steps with the live count
                    sortedStepsMap.put(todayDateKey, todaysSteps);

                    boolean isDataEmpty = true;
                    for (int steps : sortedStepsMap.values()) {
                        if (steps > 0) {
                            isDataEmpty = false;
                            break;
                        }
                    }

                    if (isDataEmpty) {
                        view.post(() -> {
                            if (isAdded()) {
                                showLoadingState(false);
                                showEmptyState(true);
                            }
                        });
                        return;
                    }

                    List<BarEntry> entries = new ArrayList<>();
                    List<String> labels = new ArrayList<>();
                    List<Integer> colors = new ArrayList<>();
                    int primaryColor = getThemeColor(com.google.android.material.R.attr.colorPrimary);
                    int accentColor = getThemeColor(com.google.android.material.R.attr.colorPrimaryContainer);

                    int i = 0;
                    // The TreeMap ensures the entries are sorted by date
                    for (Map.Entry<String, Integer> entry : sortedStepsMap.entrySet()) {
                        entries.add(new BarEntry(i, entry.getValue()));
                        labels.add(formatDateLabel(entry.getKey()));
                        // Highlight today's bar
                        colors.add(entry.getKey().equals(todayDateKey) ? primaryColor : accentColor);
                        i++;
                    }

                    view.post(() -> {
                        if (!isAdded()) return;

                        showLoadingState(false);
                        showEmptyState(false);

                        barChart.getXAxis().setValueFormatter(new ValueFormatter() {
                            @Override
                            public String getFormattedValue(float value) {
                                int index = (int) value;
                                if (index >= 0 && index < labels.size()) {
                                    return labels.get(index);
                                }
                                return "";
                            }
                        });

                        BarDataSet dataSet = new BarDataSet(entries, "Steps");
                        dataSet.setColors(colors);
                        dataSet.setValueTextColor(getThemeColor(com.google.android.material.R.attr.colorOnSurface));
                        dataSet.setValueTextSize(10f);
                        dataSet.setDrawValues(false);

                        BarData barData = new BarData(dataSet);
                        if (currentDays == 30) {
                            barData.setBarWidth(0.8f);
                            barChart.getXAxis().setLabelCount(5, true);
                        } else {
                            barData.setBarWidth(0.5f);
                            barChart.getXAxis().setLabelCount(7, false);
                        }

                        barChart.setData(barData);
                        barChart.animateY(500, Easing.EaseInOutCubic);
                        barChart.invalidate();
                    });
                }
            });
        });
    }

    private String formatDateLabel(String dateStr) {
        try {
            SimpleDateFormat sdfIn = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
            Date date = sdfIn.parse(dateStr);
            SimpleDateFormat sdfOut = new SimpleDateFormat("EEE", Locale.getDefault());
            // Special label for today
            if (dateStr.equals(new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date()))) {
                return "Today";
            }
            if (currentDays == 30) {
                SimpleDateFormat sdfMonth = new SimpleDateFormat("MMM dd", Locale.getDefault());
                return sdfMonth.format(date);
            }
            return sdfOut.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return "";
        }
    }

    private void showLoadingState(boolean isLoading) {
        if (!isAdded()) return;
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) barChart.setVisibility(View.INVISIBLE);
        emptyTextView.setVisibility(View.GONE);
    }

    private void showEmptyState(boolean show) {
        if (!isAdded()) return;
        barChart.setVisibility(show ? View.GONE : View.VISIBLE);
        emptyTextView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getContext() != null) {
            updateChartData();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Clear the chart data. This is crucial to prevent showing stale data
        // if the user logs out and a new user logs in, as the fragment instance might be reused.
        // onResume() will then call updateChartData() to fetch fresh data for the correct user.
        if (barChart != null && barChart.getData() != null) {
            barChart.clear();
            barChart.invalidate();
        }
    }



    private int getThemeColor(@AttrRes int colorAttr) {
        if(getContext() == null) return 0;
        TypedValue typedValue = new TypedValue();
        if (requireContext().getTheme().resolveAttribute(colorAttr, typedValue, true)) {
            if (typedValue.resourceId != 0) {
                return ContextCompat.getColor(requireContext(), typedValue.resourceId);
            } else {
                return typedValue.data;
            }
        }
        return ContextCompat.getColor(requireContext(), R.color.brand_primary);
    }

}
