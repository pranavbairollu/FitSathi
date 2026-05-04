package com.example.fitsathi.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitsathi.R;
import com.example.fitsathi.adapters.FoodLogAdapter;
import com.example.fitsathi.databinding.FragmentBarChartBinding;
import com.example.fitsathi.managers.FoodLogManager;
import com.example.fitsathi.models.FoodItem;
import com.example.fitsathi.viewmodels.BarChartViewModel;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.util.List;

public class BarChartFragment extends Fragment {

    private FragmentBarChartBinding binding;
    private BarChartViewModel viewModel;
    private List<String> fullDateLabels;
    private int currentDays = 7;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentBarChartBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(BarChartViewModel.class);

        setupChartStyle();
        observeViewModel();
        setupChartListener();
        setupToggleListener();

        updateChartData();
    }

    private void setupToggleListener() {
        binding.toggleButtonGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.week_button) {
                    currentDays = 7;
                } else if (checkedId == R.id.month_button) {
                    currentDays = 30;
                }
                updateChartData();
            }
        });
    }

    private void updateChartData() {
        binding.barChart.clear();
        binding.barChart.setNoDataText("Loading data...");
        viewModel.loadCalorieData(requireContext(), currentDays);
    }

    private void setupChartListener() {
        binding.barChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                if (fullDateLabels != null) {
                    int index = (int) e.getX();
                    if (index >= 0 && index < fullDateLabels.size()) {
                        String date = fullDateLabels.get(index);
                        showFoodLogDialog(date);
                    }
                }
            }

            @Override
            public void onNothingSelected() {

            }
        });
    }

    private void showFoodLogDialog(String date) {
        FoodLogManager.getFoodListForDate(date, foodItems -> {
            if (isAdded() && getActivity() != null) { // Ensure fragment is still attached and activity is available
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                // Use the activity's layout inflater
                LayoutInflater inflater = getActivity().getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.dialog_food_log_details, null);
                builder.setView(dialogView);

                TextView dialogTitle = dialogView.findViewById(R.id.dialog_title);
                RecyclerView recyclerView = dialogView.findViewById(R.id.food_log_recycler_view);
                TextView emptyView = dialogView.findViewById(R.id.empty_view);
                Button closeButton = dialogView.findViewById(R.id.close_button);

                dialogTitle.setText("Log for " + date);

                if (foodItems == null || foodItems.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyView.setVisibility(View.GONE);
                    // The context for the LayoutManager should also be consistent
                    recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
                    FoodLogAdapter adapter = new FoodLogAdapter(foodItems);
                    recyclerView.setAdapter(adapter);
                }

                final AlertDialog dialog = builder.create();
                closeButton.setOnClickListener(v -> dialog.dismiss());
                dialog.show();
            }
        });
    }


    private void setupChartStyle() {
        binding.barChart.getDescription().setEnabled(false);
        binding.barChart.getLegend().setEnabled(false);
        binding.barChart.setDrawGridBackground(false);
        binding.barChart.setFitBars(true);
        binding.barChart.setNoDataText("Loading weekly data...");
        binding.barChart.setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        binding.barChart.animateY(1000);

        XAxis xAxis = binding.barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));

        YAxis leftAxis = binding.barChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        leftAxis.setGridColor(ContextCompat.getColor(requireContext(), R.color.outline));
        binding.barChart.getAxisRight().setEnabled(false);
    }

    private void observeViewModel() {
        viewModel.getBarData().observe(getViewLifecycleOwner(), barData -> {
            if (barData != null) {
                binding.barChart.setData(barData);
                binding.barChart.invalidate();
            }
        });

        viewModel.getXAxisLabels().observe(getViewLifecycleOwner(), labels -> {
            if (currentDays == 30) {
                binding.barChart.getXAxis().setLabelCount(5, true);
            } else {
                binding.barChart.getXAxis().setLabelCount(7, false);
            }
            binding.barChart.getXAxis().setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    int index = (int) value;
                    if (index >= 0 && index < labels.size()) {
                        return labels.get(index);
                    }
                    return "";
                }
            });
        });

        viewModel.getFullDateLabels().observe(getViewLifecycleOwner(), labels -> this.fullDateLabels = labels);

        viewModel.getYAxisMax().observe(getViewLifecycleOwner(), yMax ->
                binding.barChart.getAxisLeft().setAxisMaximum(yMax));

        viewModel.getLimitLines().observe(getViewLifecycleOwner(), limitLines -> {
            YAxis leftAxis = binding.barChart.getAxisLeft();
            leftAxis.removeAllLimitLines();
            for (com.github.mikephil.charting.components.LimitLine line : limitLines) {
                leftAxis.addLimitLine(line);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
