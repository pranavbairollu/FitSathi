package com.example.fitsathi;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.fitsathi.managers.WorkoutHistoryManager;
import com.example.fitsathi.models.Exercise;
import com.example.fitsathi.models.WorkoutSession;
import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class WorkoutEngineStressTest {

    @Mock
    Context mockContext;
    @Mock
    SharedPreferences mockPrefs;
    @Mock
    SharedPreferences.Editor mockEditor;

    private WorkoutHistoryManager historyManager;
    private Gson gson;
    private Map<String, String> prefsMap;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        gson = new Gson();
        prefsMap = new HashMap<>();

        when(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs);
        when(mockPrefs.edit()).thenReturn(mockEditor);
        when(mockEditor.putString(anyString(), anyString())).thenAnswer(invocation -> {
            prefsMap.put(invocation.getArgument(0), invocation.getArgument(1));
            return mockEditor;
        });
        when(mockEditor.remove(anyString())).thenAnswer(invocation -> {
            prefsMap.remove(invocation.getArgument(0));
            return mockEditor;
        });
        when(mockPrefs.getString(anyString(), anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return prefsMap.containsKey(key) ? prefsMap.get(key) : invocation.getArgument(1);
        });

        historyManager = new WorkoutHistoryManager(mockContext);
    }

    @Test
    public void testSessionPersistence() {
        // 1. Create a session
        List<Exercise> exercises = new ArrayList<>();
        Exercise ex = new Exercise();
        ex.name = "Test Pushup";
        ex.duration = 5;
        ex.durationSec = 300;
        exercises.add(ex);

        WorkoutSession originalSession = new WorkoutSession("2026-05-04", exercises);
        originalSession.currentExerciseIndex = 0;
        originalSession.remainingSeconds = 150; // Halfway done
        originalSession.isPaused = true;

        // 2. Save session
        historyManager.saveCurrentSession(originalSession);

        // 3. Load session
        WorkoutSession loadedSession = historyManager.loadCurrentSession();

        // 4. Verify integrity
        assertNotNull(loadedSession);
        assertEquals(originalSession.date, loadedSession.date);
        assertEquals(originalSession.remainingSeconds, loadedSession.remainingSeconds);
        assertEquals(originalSession.isPaused, loadedSession.isPaused);
        assertEquals(originalSession.exercises.get(0).name, loadedSession.exercises.get(0).name);
    }

    @Test
    public void testHistoryPruning() {
        // Stress test: Add 200 exercises (manager should prune to 100)
        for (int i = 0; i < 200; i++) {
            Exercise ex = new Exercise();
            ex.name = "Exercise " + i;
            historyManager.addCompletedExercise(ex);
        }

        List<Exercise> history = historyManager.getWorkoutHistory();
        assertEquals(100, history.size());
        assertEquals("Exercise 199", history.get(99).name);
    }

    @Test
    public void testAtomicCompletionTracking() {
        Exercise ex = new Exercise();
        ex.name = "Squat";
        ex.duration = 10;
        
        historyManager.addCompletedExercise(ex);
        List<Exercise> history = historyManager.getWorkoutHistory();
        
        assertFalse(history.isEmpty());
        assertTrue(history.get(0).completionTimestamp > 0);
    }

    @Test
    public void testEmptySessionHandling() {
        historyManager.clearCurrentSession();
        WorkoutSession session = historyManager.loadCurrentSession();
        assertNull(session);
    }
}
