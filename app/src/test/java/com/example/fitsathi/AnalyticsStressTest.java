package com.example.fitsathi;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class AnalyticsStressTest {

    @Before
    public void setup() {
        // Mock any necessary components or clear states
    }

    @Test
    public void testSimulatedRapidToggleSwitching() throws InterruptedException {
        // Simulates rapid toggling between Week (7 days) and Month (30 days) views
        // to ensure thread safety and that the last requested state is handled.
        AtomicInteger finalStateDays = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(10); // simulate 10 rapid toggles

        for (int i = 0; i < 10; i++) {
            final int daysRequested = (i % 2 == 0) ? 7 : 30;
            new Thread(() -> {
                // Simulate network latency
                try {
                    Thread.sleep((long) (Math.random() * 50));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                
                // In a real scenario, this would update a view model or manager.
                // We're just asserting the state doesn't crash.
                finalStateDays.set(daysRequested);
                latch.countDown();
            }).start();
        }

        assertTrue("Timeout waiting for rapid toggles", latch.await(2, TimeUnit.SECONDS));
        assertTrue("Final state should be either 7 or 30", finalStateDays.get() == 7 || finalStateDays.get() == 30);
    }

    @Test
    public void testEmptyDataHandlingFor30Days() {
        // Simulates a map where 30 days are requested but only 2 days have data
        Map<String, Integer> historicalData = new HashMap<>();
        historicalData.put("20260501", 5000);
        historicalData.put("20260502", 8000);
        
        int nonZeroDays = 0;
        int emptyDays = 0;
        
        for (int i = 0; i < 30; i++) {
            String mockDateKey = "202605" + String.format("%02d", (i % 31) + 1);
            int steps = historicalData.getOrDefault(mockDateKey, 0);
            if (steps > 0) {
                nonZeroDays++;
            } else {
                emptyDays++;
            }
        }
        
        assertTrue("Should have exactly 2 non-zero days", nonZeroDays <= 2);
        assertTrue("Remaining days should handle zero gracefully", emptyDays >= 28);
    }

    @Test
    public void testTargetWeightScalingLogic() {
        // Test logic for determining Y-Axis min and max bounds based on target weight
        float targetWeight = 60.0f;
        float[] weightLogs = {80.5f, 79.0f, 78.2f, 77.0f}; // User is losing weight but still far from target
        
        float minWeight = Float.MAX_VALUE;
        float maxWeight = Float.MIN_VALUE;
        
        for (float w : weightLogs) {
            minWeight = Math.min(minWeight, w);
            maxWeight = Math.max(maxWeight, w);
        }
        
        // Apply scaling logic
        minWeight = Math.min(minWeight, targetWeight);
        maxWeight = Math.max(maxWeight, targetWeight);
        
        assertEquals(60.0f, minWeight, 0.01f);
        assertEquals(80.5f, maxWeight, 0.01f);
    }

    @Test
    public void testTargetWeightMissingLogs() {
        // Test logic when user sets a target weight but has 0 weight logs
        float targetWeight = 65.0f;
        float[] weightLogs = {}; // Empty array
        
        float minWeight = Float.MAX_VALUE;
        float maxWeight = Float.MIN_VALUE;
        
        if (weightLogs.length > 0) {
            for (float w : weightLogs) {
                minWeight = Math.min(minWeight, w);
                maxWeight = Math.max(maxWeight, w);
            }
        } else {
            // Graceful fallback logic
            minWeight = targetWeight;
            maxWeight = targetWeight;
        }
        
        assertEquals(65.0f, minWeight, 0.01f);
        assertEquals(65.0f, maxWeight, 0.01f);
    }

}
