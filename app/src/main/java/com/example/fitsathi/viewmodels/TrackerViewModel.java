package com.example.fitsathi.viewmodels;

import android.content.Context;
import android.util.Log;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.fitsathi.R;
import com.example.fitsathi.managers.FoodLogManager;
import com.example.fitsathi.managers.NutritionixManager;
import com.example.fitsathi.managers.OpenFoodFactsManager;
import com.example.fitsathi.managers.StepCounterManager;
import com.example.fitsathi.managers.UserInfoManager;
import com.example.fitsathi.models.FoodItem;
import com.example.fitsathi.utils.DateUtils;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrackerViewModel extends ViewModel {

    private static final String TAG = "TrackerViewModel";

    private final MutableLiveData<List<FoodItem>> foodList = new MutableLiveData<>();
    private final MutableLiveData<PieDataSet> pieData = new MutableLiveData<>();
    private final MutableLiveData<Map<String, String>> summaryText = new MutableLiveData<>();
    private final MutableLiveData<Map<String, String>> streakAndGoalText = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private final MutableLiveData<String> foodInputError = new MutableLiveData<>();
    private final MutableLiveData<String> mealTypeError = new MutableLiveData<>();
    private final MutableLiveData<String> navigateToManualEntry = new MutableLiveData<>();

    public LiveData<List<FoodItem>> getFoodList() { return foodList; }
    public LiveData<PieDataSet> getPieData() { return pieData; }
    public LiveData<Map<String, String>> getSummaryText() { return summaryText; }
    public LiveData<Map<String, String>> getStreakAndGoalText() { return streakAndGoalText; }
    public LiveData<String> getToastMessage() { return toastMessage; }
    public LiveData<String> getFoodInputError() { return foodInputError; }
    public LiveData<String> getMealTypeError() { return mealTypeError; }
    public LiveData<String> getNavigateToManualEntry() { return navigateToManualEntry; }

    public void loadInitialData(Context context) {
        String today = DateUtils.getTodayDateUTC_forFoodLogs(); // Use UTC date for loading
        FoodLogManager.getFoodListForDate(today, items -> {
            foodList.setValue(items);
            updateSummaryText(items);
            UserInfoManager.getUserInfo(userInfo -> {
                updateConsumedVsRemainingPieChart(context, items, userInfo);
                updateStreakAndGoalText(context, userInfo);
            });
        });
    }

    private void updateSummaryText(List<FoodItem> items) {
        double totalProtein = 0, totalCarbs = 0, totalFat = 0;
        for (FoodItem item : items) {
            totalProtein += item.getProtein();
            totalCarbs += item.getCarbs();
            totalFat += item.getFat();
        }
        Map<String, String> summary = new HashMap<>();
        summary.put("protein", String.format("%.0f g", totalProtein));
        summary.put("carbs", String.format("%.0f g", totalCarbs));
        summary.put("fat", String.format("%.0f g", totalFat));
        summaryText.setValue(summary);
    }

    private void updateStreakAndGoalText(Context context, UserInfoManager.UserInfo userInfo) {
        UserInfoManager.getStreak(streak -> {
            double calorieGoal = UserInfoManager.calculateDailyCalorieGoal(userInfo);
            Map<String, String> info = new HashMap<>();
            info.put("streak", "🔥 " + streak);
            info.put("goal", context.getString(R.string.goal_kcal, (int) calorieGoal));
            streakAndGoalText.setValue(info);
        });
    }

    private void updateConsumedVsRemainingPieChart(Context context, List<FoodItem> items, UserInfoManager.UserInfo userInfo) {
        double caloriesConsumed = 0;
        for (FoodItem item : items) caloriesConsumed += item.getCalories();
        double calorieGoal = UserInfoManager.calculateDailyCalorieGoal(userInfo);
        double caloriesRemaining = Math.max(0, calorieGoal - caloriesConsumed);

        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry((float) caloriesConsumed, "Consumed"));
        if (caloriesRemaining > 0) entries.add(new PieEntry((float) caloriesRemaining, "Remaining"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(ContextCompat.getColor(context, R.color.brand_primary));
        if (caloriesRemaining > 0) colors.add(ContextCompat.getColor(context, R.color.surface_variant));
        dataSet.setColors(colors);
        dataSet.setDrawValues(false);

        pieData.setValue(dataSet);
    }

    public boolean validateInputs(String query, String mealType) {
        foodInputError.setValue(null);
        mealTypeError.setValue(null);
        boolean isValid = true;
        if (query.isEmpty()) { foodInputError.setValue("Please enter a food"); isValid = false; }
        if (mealType.isEmpty()) { mealTypeError.setValue("Please select a meal type"); isValid = false; }
        return isValid;
    }

    public void fetchNutritionForFood(Context context, String query, String mealType) {
        NutritionixManager.fetchNutrition(context, query, new NutritionixManager.NutritionixCallback() {
            @Override
            public void onSuccess(FoodItem foodItem) {
                foodItem.setMealType(mealType);
                FoodLogManager.addFoodItem(DateUtils.getTodayDateUTC_forFoodLogs(), foodItem);
                loadInitialData(context);
            }
            @Override
            public void onError(String message) { toastMessage.setValue(message); }
        });
    }

    public void handleBarcodeResult(Context context, String barcode) {
        toastMessage.setValue("Searching for barcode: " + barcode);
        OpenFoodFactsManager.fetchProductByBarcode(context, barcode, new OpenFoodFactsManager.OpenFoodFactsCallback() {
            @Override
            public void onSuccess(JSONObject product) {
                String foodName = product.optString("product_name", "");
                if (foodName.isEmpty() || product.optJSONObject("nutriments") == null) {
                    onError("Product found but incomplete. Falling back.");
                    return;
                }
                FoodItem item = new FoodItem(
                        foodName,
                        product.optJSONObject("nutriments").optDouble("energy-kcal_100g", 0),
                        product.optJSONObject("nutriments").optDouble("carbohydrates_100g", 0),
                        product.optJSONObject("nutriments").optDouble("proteins_100g", 0),
                        product.optJSONObject("nutriments").optDouble("fat_100g", 0),
                        product.optJSONObject("nutriments").optDouble("fiber_100g", 0),
                        product.optJSONObject("nutriments").optDouble("sugars_100g", 0),
                        100,
                        "Snacks",
                        "OpenFoodFacts"
                );
                FoodLogManager.addFoodItem(DateUtils.getTodayDateUTC_forFoodLogs(), item);
                loadInitialData(context);
            }

            @Override
            public void onError(String message) {
                NutritionixManager.fetchNutrition(context, barcode, new NutritionixManager.NutritionixCallback() {
                    @Override
                    public void onSuccess(FoodItem foodItem) {
                        foodItem.setMealType("Snacks");
                        FoodLogManager.addFoodItem(DateUtils.getTodayDateUTC_forFoodLogs(), foodItem);
                        loadInitialData(context);
                        toastMessage.setValue("Found via search: " + foodItem.getName());
                    }
                    @Override
                    public void onError(String errorMessage) {
                        navigateToManualEntry.setValue(barcode);
                        toastMessage.setValue("Product not found. Please add manually.");
                    }
                });
            }
        });
    }

    public void removeFoodItem(Context context, FoodItem foodItem) {
        FoodLogManager.removeFoodItem(DateUtils.getTodayDateUTC_forFoodLogs(), foodItem.getKey());
        loadInitialData(context);
    }

    public void updateFoodItem(Context context, FoodItem foodItem) {
        FoodLogManager.updateFoodItem(DateUtils.getTodayDateUTC_forFoodLogs(), foodItem);
        loadInitialData(context);
    }

    public void clearToastMessage() { toastMessage.setValue(null); }
    public void doneNavigating() { navigateToManualEntry.setValue(null); }
}
