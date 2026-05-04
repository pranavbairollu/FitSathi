package com.example.fitsathi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.example.fitsathi.managers.UserInfoManager;

import org.junit.Test;

public class OnboardingStressTest {

    @Test
    public void testCalorieCalculation_Male_Sedentary_Maintain() {
        UserInfoManager.UserInfo userInfo = new UserInfoManager.UserInfo();
        userInfo.setGender("Male");
        userInfo.setAge(25);
        userInfo.setHeight(180);
        userInfo.setWeight(75);
        userInfo.setActivityLevel("Sedentary");
        userInfo.setFitnessGoal("Maintain");

        double bmr = 88.362 + (13.397 * 75) + (4.799 * 180) - (5.677 * 25); // ~1815
        double tdee = bmr * 1.2; // ~2178
        
        double calculated = UserInfoManager.calculateDailyCalorieGoal(userInfo);
        assertEquals(tdee, calculated, 1.0);
    }

    @Test
    public void testCalorieCalculation_Female_Active_LoseWeight() {
        UserInfoManager.UserInfo userInfo = new UserInfoManager.UserInfo();
        userInfo.setGender("Female");
        userInfo.setAge(30);
        userInfo.setHeight(165);
        userInfo.setWeight(60);
        userInfo.setActivityLevel("Very Active");
        userInfo.setFitnessGoal("Lose Weight");

        double bmr = 447.593 + (9.247 * 60) + (3.098 * 165) - (4.330 * 30); // ~1383
        double tdee = bmr * 1.725; // ~2386
        double expected = tdee - 500; // ~1886
        
        double calculated = UserInfoManager.calculateDailyCalorieGoal(userInfo);
        assertEquals(expected, calculated, 1.0);
    }

    @Test
    public void testCalorieCalculation_Male_ExtraActive_BuildMuscle() {
        UserInfoManager.UserInfo userInfo = new UserInfoManager.UserInfo();
        userInfo.setGender("Male");
        userInfo.setAge(20);
        userInfo.setHeight(185);
        userInfo.setWeight(80);
        userInfo.setActivityLevel("Extra Active");
        userInfo.setFitnessGoal("Build Muscle");

        double bmr = 88.362 + (13.397 * 80) + (4.799 * 185) - (5.677 * 20); // ~1934
        double tdee = bmr * 1.9; // ~3674
        double expected = tdee + 300; // ~3974
        
        double calculated = UserInfoManager.calculateDailyCalorieGoal(userInfo);
        assertEquals(expected, calculated, 1.0);
    }

    @Test
    public void testCalorieCalculation_EdgeCase_Nulls() {
        UserInfoManager.UserInfo userInfo = new UserInfoManager.UserInfo();
        // Should use defaults: Sedentary, Maintain, 2000 BMR if 0s
        double calculated = UserInfoManager.calculateDailyCalorieGoal(userInfo);
        assertEquals(2000 * 1.2, calculated, 0.1);
    }
}
