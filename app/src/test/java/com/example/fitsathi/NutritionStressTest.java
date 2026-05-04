package com.example.fitsathi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.fitsathi.managers.UserInfoManager;
import com.example.fitsathi.models.FoodItem;
import com.example.fitsathi.utils.DateUtils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class NutritionStressTest {

    @Test
    public void testMacroSummationLogic() {
        List<FoodItem> items = new ArrayList<>();
        items.add(new FoodItem("Apple", 52, 14, 0.3, 0.2, 2.4, 10, 100, "Snack", "Nutritionix"));
        items.add(new FoodItem("Chicken Breast", 165, 0, 31, 3.6, 0, 0, 100, "Lunch", "Nutritionix"));
        
        double totalCalories = 0;
        double totalProtein = 0;
        double totalCarbs = 0;
        double totalFat = 0;
        double totalFiber = 0;
        double totalSugar = 0;

        for (FoodItem item : items) {
            totalCalories += item.getCalories();
            totalProtein += item.getProtein();
            totalCarbs += item.getCarbs();
            totalFat += item.getFat();
            totalFiber += item.getFibre();
            totalSugar += item.getSugar();
        }

        assertEquals(217, totalCalories, 0.1);
        assertEquals(31.3, totalProtein, 0.1);
        assertEquals(14, totalCarbs, 0.1);
        assertEquals(3.8, totalFat, 0.1);
        assertEquals(2.4, totalFiber, 0.1);
        assertEquals(10, totalSugar, 0.1);
    }

    @Test
    public void testCalorieGoalCalculation() {
        UserInfoManager.UserInfo userInfo = new UserInfoManager.UserInfo();
        userInfo.setAge(25);
        userInfo.setWeight(70);
        userInfo.setHeight(175);
        userInfo.setGender("Male");
        userInfo.setActivityLevel("Sedentary");
        userInfo.setFitnessGoal("Maintain Weight");

        double goal = UserInfoManager.calculateDailyCalorieGoal(userInfo);
        // BMR for Male: 88.362 + (13.397 * 70) + (4.799 * 175) - (5.677 * 25) = 1724.0
        // TDEE (Sedentary): 1724 * 1.2 = 2068.8
        assertTrue(goal > 2000 && goal < 2100);
    }

    @Test
    public void testDateLogicConsistency() {
        String logDate = DateUtils.getFoodLogDate();
        String todayDate = DateUtils.getTodayDate();
        
        // Both should be the same in local time
        assertEquals(todayDate, logDate);
        assertTrue(logDate.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void testExtremeServingSizeHandling() {
        FoodItem item = new FoodItem("Test", 100, 20, 10, 5, 2, 8, 100, "Snack", "Test");
        
        // Scale to 500g
        double newSize = 500;
        double scale = newSize / 100.0;
        
        assertEquals(500, item.getCalories() * scale, 0.1);
        assertEquals(100, item.getCarbs() * scale, 0.1);
        assertEquals(50, item.getProtein() * scale, 0.1);
    }
}
