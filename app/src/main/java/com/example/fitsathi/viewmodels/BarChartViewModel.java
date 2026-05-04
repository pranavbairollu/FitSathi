package com.example.fitsathi.viewmodels;

import android.content.Context;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.fitsathi.R;
import com.example.fitsathi.managers.FoodLogManager;
import com.example.fitsathi.managers.UserInfoManager;
import com.example.fitsathi.models.FoodItem;
import com.example.fitsathi.utils.DateUtils;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class BarChartViewModel extends ViewModel {

    private final MutableLiveData<BarData> barData = new MutableLiveData<>();
    public LiveData<BarData> getBarData() { return barData; }

    private final MutableLiveData<List<String>> xAxisLabels = new MutableLiveData<>();
    public LiveData<List<String>> getXAxisLabels() { return xAxisLabels; }

    private final MutableLiveData<List<String>> fullDateLabels = new MutableLiveData<>();
    public LiveData<List<String>> getFullDateLabels() { return fullDateLabels; }

    private final MutableLiveData<Float> yAxisMax = new MutableLiveData<>();
    public LiveData<Float> getYAxisMax() { return yAxisMax; }

    private final MutableLiveData<List<LimitLine>> limitLines = new MutableLiveData<>();
    public LiveData<List<LimitLine>> getLimitLines() { return limitLines; }

    private String formatDate(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date);
    }

    public void loadWeeklyCalorieData(Context context) {
        UserInfoManager.getUserInfo(userInfo -> {
            if (userInfo == null) {
                return;
            }

            float calorieGoal = (float) UserInfoManager.calculateDailyCalorieGoal(userInfo);
            final int totalDays = 7;
            Map<String, List<FoodItem>> weeklyFoodData = new ConcurrentHashMap<>();
            AtomicInteger daysProcessed = new AtomicInteger(0);

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -(totalDays - 1));

            for (int i = 0; i < totalDays; i++) {
                String date = formatDate(cal.getTime());
                FoodLogManager.getFoodListForDate(date, foodList -> {
                    weeklyFoodData.put(date, foodList);
                    if (daysProcessed.incrementAndGet() == totalDays) {
                        processAndPostChartData(context, weeklyFoodData, calorieGoal);
                    }
                });
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }
        });
    }

    private void processAndPostChartData(Context context, Map<String, List<FoodItem>> weeklyFoodData, float calorieGoal) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> shortLabels = new ArrayList<>();
        List<String> fullLabels = new ArrayList<>();
        float totalCalories = 0f;
        float maxCalorieValue = 0f;

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -6);

        for (int i = 0; i < 7; i++) {
            String fullDate = formatDate(cal.getTime());
            List<FoodItem> foodList = weeklyFoodData.get(fullDate);

            double dailyTotal = 0;
            if (foodList != null) {
                for (FoodItem item : foodList) dailyTotal += item.getCalories();
            }

            float roundedTotal = (float) Math.round(dailyTotal);
            entries.add(new BarEntry(i, roundedTotal));
            shortLabels.add(DateUtils.formatShortLabel(cal.getTime()));
            fullLabels.add(fullDate);

            totalCalories += roundedTotal;
            if (roundedTotal > maxCalorieValue) maxCalorieValue = roundedTotal;

            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        BarDataSet dataSet = new BarDataSet(entries, "Calories");
        dataSet.setColors(getBarColors(context, entries, calorieGoal));
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(ContextCompat.getColor(context, R.color.on_surface));
        dataSet.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf(Math.round(value));
            }
        });

        BarData finalBarData = new BarData(dataSet);
        finalBarData.setBarWidth(0.5f);

        float weeklyAverage = totalCalories / 7f;
        List<LimitLine> lines = new ArrayList<>();

        LimitLine goalLine = new LimitLine(calorieGoal, "Goal: " + (int) calorieGoal);
        goalLine.setLineWidth(2f);
        goalLine.setLineColor(ContextCompat.getColor(context, R.color.brand_primary));
        goalLine.setTextColor(ContextCompat.getColor(context, R.color.on_surface));
        goalLine.setTextSize(10f);
        goalLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        lines.add(goalLine);

        LimitLine avgLine = new LimitLine(weeklyAverage, "Avg: " + (int) weeklyAverage);
        avgLine.setLineWidth(1.5f);
        avgLine.setLineColor(ContextCompat.getColor(context, R.color.brand_secondary));
        avgLine.setTextColor(ContextCompat.getColor(context, R.color.on_surface));
        avgLine.setTextSize(10f);
        avgLine.enableDashedLine(10f, 10f, 0f);
        avgLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        lines.add(avgLine);
        
        barData.postValue(finalBarData);
        xAxisLabels.postValue(shortLabels);
        fullDateLabels.postValue(fullLabels);
        yAxisMax.postValue(Math.max(maxCalorieValue, calorieGoal) * 1.2f);
        limitLines.postValue(lines);
    }


    private List<Integer> getBarColors(Context context, List<BarEntry> entries, float goal) {
        List<Integer> colors = new ArrayList<>();
        int colorOver = ContextCompat.getColor(context, R.color.error);
        int colorNear = ContextCompat.getColor(context, R.color.brand_primary);
        int colorUnder = ContextCompat.getColor(context, R.color.brand_secondary);
        int colorZero = ContextCompat.getColor(context, R.color.surface_variant);

        for (BarEntry e : entries) {
            float val = e.getY();
            if (val == 0) colors.add(colorZero);
            else if (val > goal * 1.1) colors.add(colorOver);
            else if (val >= goal * 0.9) colors.add(colorNear);
            else colors.add(colorUnder);
        }
        return colors;
    }
}
