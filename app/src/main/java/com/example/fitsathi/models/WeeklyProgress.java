package com.example.fitsathi.models;

import java.util.Map;

public class WeeklyProgress {
    private Map<String, Integer> stepsHistory;
    private Map<String, Integer> waterHistory;
    private Map<String, Integer> calorieHistory;
    private int totalWorkouts;
    private String startDate;
    private String endDate;
    private String userName;
    private String profilePicUrl;

    public WeeklyProgress() {}

    public Map<String, Integer> getStepsHistory() { return stepsHistory; }
    public void setStepsHistory(Map<String, Integer> stepsHistory) { this.stepsHistory = stepsHistory; }

    public Map<String, Integer> getWaterHistory() { return waterHistory; }
    public void setWaterHistory(Map<String, Integer> waterHistory) { this.waterHistory = waterHistory; }

    public Map<String, Integer> getCalorieHistory() { return calorieHistory; }
    public void setCalorieHistory(Map<String, Integer> calorieHistory) { this.calorieHistory = calorieHistory; }

    public int getTotalWorkouts() { return totalWorkouts; }
    public void setTotalWorkouts(int totalWorkouts) { this.totalWorkouts = totalWorkouts; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getProfilePicUrl() { return profilePicUrl; }
    public void setProfilePicUrl(String profilePicUrl) { this.profilePicUrl = profilePicUrl; }

    public int getTotalSteps() {
        int total = 0;
        if (stepsHistory != null) {
            for (int steps : stepsHistory.values()) total += steps;
        }
        return total;
    }

    public int getTotalWater() {
        int total = 0;
        if (waterHistory != null) {
            for (int water : waterHistory.values()) total += water;
        }
        return total;
    }

    public int getAverageCalories() {
        if (calorieHistory == null || calorieHistory.isEmpty()) return 0;
        int total = 0;
        for (int cal : calorieHistory.values()) total += cal;
        return total / calorieHistory.size();
    }
}
