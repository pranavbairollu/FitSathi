package com.example.fitsathi.fragments;

import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.camera.core.ExperimentalGetImage;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.fitsathi.R;
import com.example.fitsathi.adapters.FoodAdapter;
import com.example.fitsathi.databinding.FragmentTrackerBinding;
import com.example.fitsathi.models.FoodItem;
import com.example.fitsathi.viewmodels.TrackerViewModel;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class TrackerFragment extends Fragment implements EditFoodDialogFragment.OnFoodItemUpdatedListener {

    private FragmentTrackerBinding binding;
    private TrackerViewModel viewModel;
    private FoodAdapter foodAdapter;
    private static final String[] MEAL_TYPES = {"Breakfast", "Lunch", "Dinner", "Snacks"};
    private static final String TAG = "TrackerFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTrackerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TrackerViewModel.class);

        setupUI();
        setupPieChart();
        setupClickListeners();
        setupFragmentResultListener();
        observeViewModel();

        // Data is now loaded in onResume to ensure it's always fresh
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data every time the fragment becomes visible to ensure UI is always up-to-date
        viewModel.loadInitialData(requireContext());
    }

    private void setupUI() {
        foodAdapter = new FoodAdapter();
        binding.foodRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.foodRecyclerView.setAdapter(foodAdapter);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, MEAL_TYPES);
        binding.mealTypeSpinner.setAdapter(spinnerAdapter);
    }

    private int getThemeColor(@AttrRes int attrRes) {
        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(attrRes, typedValue, true);
        return typedValue.data;
    }

    private void setupPieChart() {
        binding.pieChart.setUsePercentValues(false);
        binding.pieChart.getDescription().setEnabled(false);
        binding.pieChart.setDrawHoleEnabled(true);
        binding.pieChart.setHoleColor(Color.TRANSPARENT);
        binding.pieChart.setTransparentCircleRadius(0f);
        binding.pieChart.setRotationEnabled(false);
        binding.pieChart.setHighlightPerTapEnabled(false);
        binding.pieChart.setCenterTextSize(12f);
        binding.pieChart.setCenterTextColor(getThemeColor(com.google.android.material.R.attr.colorOnSurface));
        binding.pieChart.setDrawEntryLabels(false); // This disables the slice labels

        Legend legend = binding.pieChart.getLegend();
        legend.setEnabled(true);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setXEntrySpace(15f);
        legend.setYOffset(10f);
        legend.setTextColor(getThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
    }

    private void setupClickListeners() {
        foodAdapter.setOnItemClickListener(position -> {
            FoodItem item = foodAdapter.getFoodItemAt(position);
            EditFoodDialogFragment dialogFragment = EditFoodDialogFragment.newInstance(item);
            dialogFragment.setOnFoodItemUpdatedListener(this);
            dialogFragment.show(getParentFragmentManager(), "edit_food_dialog");
        });

        foodAdapter.setOnItemLongClickListener(position -> {
            FoodItem item = foodAdapter.getFoodItemAt(position);
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Remove Item?")
                    .setMessage("Are you sure you want to remove " + item.getName() + "?")
                    .setPositiveButton("Remove", (dialog, which) -> viewModel.removeFoodItem(requireContext(), item))
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        binding.addButton.setOnClickListener(v -> {
            String query = binding.foodInput.getText().toString().trim();
            String mealType = binding.mealTypeSpinner.getText().toString();

            if (viewModel.validateInputs(query, mealType)) {
                viewModel.fetchNutritionForFood(requireContext(), query, mealType);
                binding.foodInput.setText("");
            }
        });

        binding.scanFab.setOnClickListener(v -> startBarcodeScanner());
    }

    private void setupFragmentResultListener() {
        getParentFragmentManager().setFragmentResultListener("barcode_request", this, (requestKey, bundle) -> {
            String barcode = bundle.getString("barcode_result");
            if (barcode != null) {
                Log.d(TAG, "Received barcode from CameraFragment: " + barcode);
                viewModel.handleBarcodeResult(requireContext(), barcode);
            }
        });
    }

    private void observeViewModel() {
        viewModel.getFoodList().observe(getViewLifecycleOwner(), foodItems -> {
            foodAdapter.submitList(foodItems);
            binding.emptyLogView.setVisibility(foodItems.isEmpty() ? View.VISIBLE : View.GONE);
            binding.foodRecyclerView.setVisibility(foodItems.isEmpty() ? View.GONE : View.VISIBLE);
        });

        viewModel.getPieData().observe(getViewLifecycleOwner(), pieDataSet -> {
            if (pieDataSet != null && pieDataSet.getEntryCount() > 0) {
                float consumedValue = pieDataSet.getEntryForIndex(0).getValue();
                float remainingValue = pieDataSet.getEntryCount() > 1 ? pieDataSet.getEntryForIndex(1).getValue() : 0f;
                float goalValue = consumedValue + remainingValue;

                // Update Pie Chart
                PieData pieData = new PieData(pieDataSet);
                pieData.setDrawValues(false);
                binding.pieChart.setData(pieData);
                binding.pieChart.setCenterText(String.format(getString(R.string.consumed_kcal), consumedValue));
                binding.pieChart.animateY(1000);

                // Update Linear Progress Bar (Status Bar)
                int progress = (int) (goalValue > 0 ? (consumedValue / goalValue) * 100 : 0);
                binding.calorieProgressBar.setProgress(Math.min(progress, 100));
                binding.tvConsumedLabel.setText(String.format("%.0f kcal consumed", consumedValue));
                binding.tvRemainingLabel.setText(String.format("%.0f kcal left", remainingValue));
                
                // Color coding for progress bar (Red if over goal)
                if (consumedValue > goalValue && goalValue > 0) {
                    binding.calorieProgressBar.setIndicatorColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light));
                } else {
                    binding.calorieProgressBar.setIndicatorColor(getThemeColor(com.google.android.material.R.attr.colorPrimary));
                }
            } else {
                binding.pieChart.clear();
                binding.pieChart.setCenterText(String.format(getString(R.string.consumed_kcal), 0f));
                binding.calorieProgressBar.setProgress(0);
                binding.tvConsumedLabel.setText("0 kcal consumed");
                binding.tvRemainingLabel.setText("Goal not set");
            }
            binding.pieChart.invalidate();
        });

        viewModel.getSummaryText().observe(getViewLifecycleOwner(), summary -> {
            binding.proteinValue.setText(summary.get("protein"));
            binding.carbsValue.setText(summary.get("carbs"));
            binding.fatValue.setText(summary.get("fat"));
            binding.fiberValue.setText(summary.get("fiber"));
            binding.sugarValue.setText(summary.get("sugar"));
        });

        viewModel.getStreakAndGoalText().observe(getViewLifecycleOwner(), info -> {
            binding.streakText.setText(info.get("streak"));
            binding.goalText.setText(info.get("goal"));
        });

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                viewModel.clearToastMessage();
            }
        });

        viewModel.getFoodInputError().observe(getViewLifecycleOwner(), error -> binding.foodInput.setError(error));
        viewModel.getMealTypeError().observe(getViewLifecycleOwner(), error -> binding.mealTypeSpinner.setError(error));

        viewModel.getNavigateToManualEntry().observe(getViewLifecycleOwner(), barcode -> {
            if (barcode != null) {
                Log.d(TAG, "Manual entry triggered for: " + barcode);
                Toast.makeText(requireContext(), "Open manual entry for: " + barcode, Toast.LENGTH_LONG).show();
                viewModel.doneNavigating();
            }
        });

        viewModel.getShowFoodDetailDialog().observe(getViewLifecycleOwner(), foodItem -> {
            if (foodItem != null) {
                EditFoodDialogFragment dialogFragment = EditFoodDialogFragment.newInstance(foodItem);
                dialogFragment.setOnFoodItemUpdatedListener(this);
                dialogFragment.show(getParentFragmentManager(), "add_food_dialog");
                viewModel.doneShowingDetails();
            }
        });
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void startBarcodeScanner() {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new CameraFragment())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onFoodItemUpdated(FoodItem foodItem) {
        if (foodItem.getKey() == null) {
            viewModel.addFoodItem(requireContext(), foodItem);
        } else {
            viewModel.updateFoodItem(requireContext(), foodItem);
        }
    }
}