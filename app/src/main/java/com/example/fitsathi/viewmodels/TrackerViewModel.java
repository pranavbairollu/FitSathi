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
    private final MutableLiveData<FoodItem> showFoodDetailDialog = new MutableLiveData<>();

    public LiveData<List<FoodItem>> getFoodList() { return foodList; }
    public LiveData<PieDataSet> getPieData() { return pieData; }
    public LiveData<Map<String, String>> getSummaryText() { return summaryText; }
    public LiveData<Map<String, String>> getStreakAndGoalText() { return streakAndGoalText; }
    public LiveData<String> getToastMessage() { return toastMessage; }
    public LiveData<String> getFoodInputError() { return foodInputError; }
    public LiveData<String> getMealTypeError() { return mealTypeError; }
    public LiveData<String> getNavigateToManualEntry() { return navigateToManualEntry; }
    public LiveData<FoodItem> getShowFoodDetailDialog() { return showFoodDetailDialog; }

    public void loadInitialData(Context context) {
        String today = DateUtils.getFoodLogDate(); // Switch to Local Date for consistency
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
        double totalProtein = 0, totalCarbs = 0, totalFat = 0, totalFiber = 0, totalSugar = 0;
        for (FoodItem item : items) {
            totalProtein += item.getProtein();
            totalCarbs += item.getCarbs();
            totalFat += item.getFat();
            totalFiber += item.getFibre();
            totalSugar += item.getSugar();
        }
        Map<String, String> summary = new HashMap<>();
        summary.put("protein", String.format("%.0f g", totalProtein));
        summary.put("carbs", String.format("%.0f g", totalCarbs));
        summary.put("fat", String.format("%.0f g", totalFat));
        summary.put("fiber", String.format("%.0f g", totalFiber));
        summary.put("sugar", String.format("%.0f g", totalSugar));
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
                showFoodDetailDialog.setValue(foodItem);
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
                showFoodDetailDialog.setValue(item);
            }

            @Override
            public void onError(String message) {
                NutritionixManager.fetchNutrition(context, barcode, new NutritionixManager.NutritionixCallback() {
                    @Override
                    public void onSuccess(FoodItem foodItem) {
                        foodItem.setMealType("Snacks");
                        showFoodDetailDialog.setValue(foodItem);
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
        FoodLogManager.removeFoodItem(DateUtils.getFoodLogDate(), foodItem.getKey(), success -> {
            if (success) loadInitialData(context);
            else toastMessage.setValue("Failed to remove item.");
        });
    }

    public void addFoodItem(Context context, FoodItem foodItem) {
        FoodLogManager.addFoodItem(DateUtils.getFoodLogDate(), foodItem, success -> {
            if (success) loadInitialData(context);
            else toastMessage.setValue("Failed to add item.");
        });
    }

    public void updateFoodItem(Context context, FoodItem foodItem) {
        FoodLogManager.updateFoodItem(DateUtils.getFoodLogDate(), foodItem, success -> {
            if (success) loadInitialData(context);
            else toastMessage.setValue("Failed to update item.");
        });
    }

    public void clearToastMessage() { toastMessage.setValue(null); }
    public void doneNavigating() { navigateToManualEntry.setValue(null); }
    public void doneShowingDetails() { showFoodDetailDialog.setValue(null); }
}
