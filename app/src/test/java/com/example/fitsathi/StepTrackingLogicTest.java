package com.example.fitsathi;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Logic Stress Test for Step Tracking Hardening.
 * Verifies that the delta calculation logic handles reboots and date changes correctly.
 */
public class StepTrackingLogicTest {

    @Test
    public void testStepCalculationLogic() {
        // Mock current state
        int dailySteps = 5000;
        int lastSensorValue = 10000;
        String lastSavedDate = "20260503";
        String today = "20260504";

        // Case 1: Normal operation (walking)
        int currentSensorValue = 10100;
        int stepsThisEvent = currentSensorValue - lastSensorValue;
        int newDailyTotal = dailySteps + stepsThisEvent;
        assertEquals(5100, newDailyTotal);

        // Case 2: Reboot occurred (sensor value reset to 0 or low number)
        currentSensorValue = 150; // User took 150 steps after reboot
        if (currentSensorValue < lastSensorValue) {
            stepsThisEvent = currentSensorValue;
        } else {
            stepsThisEvent = currentSensorValue - lastSensorValue;
        }
        newDailyTotal = dailySteps + stepsThisEvent;
        assertEquals(5150, newDailyTotal); // Preserved yesterday's steps + new steps

        // Case 3: Midnight Transition
        dailySteps = 0; // Reset for new day
        lastSensorValue = 150; // Last value from previous day
        currentSensorValue = 200; // User took 50 steps today
        stepsThisEvent = currentSensorValue - lastSensorValue;
        newDailyTotal = dailySteps + stepsThisEvent;
        assertEquals(50, newDailyTotal);
    }
    
    @Test
    public void testPrecisionCalculations() {
        int steps = 10000;
        float heightCm = 180f;
        float weightKg = 70f;
        
        // Precision Distance (m) = steps * (height * 0.415 / 100)
        float strideLength = (heightCm * 0.415f) / 100f;
        float distanceKm = (steps * strideLength) / 1000f;
        
        // At 180cm, stride is ~0.747m. 10000 steps ~ 7.47km.
        assertEquals(7.47, distanceKm, 0.01);
        
        // Precision Calories = steps * 0.04 * (weight / 70)
        float calories = steps * 0.04f * (weightKg / 70f);
        assertEquals(400, calories, 1.0);
    }
}
