package com.example.fitsathi.managers;

import android.content.Context;
import com.example.fitsathi.R;
import com.example.fitsathi.models.WeightLog;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProjectionManager {

    public interface ProjectionCallback {
        void onProjectionCalculated(List<WeightLog> projection, double dailyChange, String reachTargetDate);
        void onError(String message);
    }

    public static void getWeightProjection(Context context, float currentWeight, float targetWeight, int projectionDays, ProjectionCallback callback) {
        UserInfoManager.getUserInfo(userInfo -> {
            if (userInfo == null) {
                callback.onError("User info not found");
                return;
            }

            double tdee = UserInfoManager.calculateTDEE(userInfo);
            
            FoodLogManager.getLastNDaysCalories(14, calorieHistory -> {
                double avgIntake = calculateAverageCalories(calorieHistory);
                
                // If no calorie data, fallback to goal-based calorie goal
                if (avgIntake <= 0) {
                    avgIntake = UserInfoManager.calculateDailyCalorieGoal(userInfo);
                }

                double dailyDeficit = tdee - avgIntake;
                // 1kg = 7700 kcal (Standard approximation for tissue weight change)
                double dailyWeightChange = dailyDeficit / 7700.0;
                
                List<WeightLog> projection = new ArrayList<>();
                long now = System.currentTimeMillis();
                long dayMillis = 24 * 60 * 60 * 1000L;

                for (int i = 1; i <= projectionDays; i++) {
                    float projectedWeight = (float) (currentWeight - (dailyWeightChange * i));
                    // Clamp weight to a minimum of 20kg to avoid unrealistic projections
                    projectedWeight = Math.max(20f, projectedWeight);
                    projection.add(new WeightLog(now + (i * dayMillis), projectedWeight));
                }

                String reachTargetDate = context.getString(R.string.projection_never);
                // Only calculate target date if a valid target weight is set
                if (targetWeight > 20) {
                    if (Math.abs(dailyWeightChange) > 0.001) {
                        double weightNeeded = currentWeight - targetWeight;
                        double daysToTarget = weightNeeded / dailyWeightChange;
                        
                        if (daysToTarget > 0 && daysToTarget < 3650) { // Within 10 years
                             java.util.Calendar cal = java.util.Calendar.getInstance();
                             cal.add(java.util.Calendar.DAY_OF_YEAR, (int) Math.ceil(daysToTarget));
                             java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
                             reachTargetDate = sdf.format(cal.getTime());
                        } else if (daysToTarget <= 0) {
                            reachTargetDate = context.getString(R.string.projection_target_reached);
                        } else {
                            reachTargetDate = dailyWeightChange > 0 ? context.getString(R.string.projection_weight_gain) : context.getString(R.string.projection_weight_loss);
                        }
                    } else {
                        reachTargetDate = context.getString(R.string.projection_stable_trend);
                    }
                } else {
                    reachTargetDate = context.getString(R.string.projection_set_target);
                }

                final String finalReachTargetDate = reachTargetDate;
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> 
                    callback.onProjectionCalculated(projection, dailyWeightChange, finalReachTargetDate)
                );
            });
        });
    }

    private static double calculateAverageCalories(Map<String, Integer> calorieHistory) {
        if (calorieHistory == null || calorieHistory.isEmpty()) return 0;
        int total = 0;
        int loggedDays = 0;
        for (Integer cal : calorieHistory.values()) {
            if (cal != null && cal > 0) {
                total += cal;
                loggedDays++;
            }
        }
        return loggedDays > 0 ? (double) total / loggedDays : 0;
    }
}
