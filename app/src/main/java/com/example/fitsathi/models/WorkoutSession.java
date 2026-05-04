package com.example.fitsathi.models;

import java.util.ArrayList;
import java.util.List;

public class WorkoutSession {
    public String date; // yyyy-MM-dd
    public List<Exercise> exercises;
    public List<Boolean> completedStatus;
    public int currentExerciseIndex;
    public long remainingSeconds;
    public boolean isPaused;

    public WorkoutSession() {
        this.exercises = new ArrayList<>();
        this.completedStatus = new ArrayList<>();
        this.currentExerciseIndex = 0;
        this.remainingSeconds = 0;
        this.isPaused = false;
    }

    public WorkoutSession(String date, List<Exercise> exercises) {
        this.date = date;
        this.exercises = exercises;
        this.completedStatus = new ArrayList<>();
        for (int i = 0; i < exercises.size(); i++) {
            this.completedStatus.add(false);
        }
        this.currentExerciseIndex = 0;
        this.remainingSeconds = exercises.isEmpty() ? 0 : exercises.get(0).durationSec;
        this.isPaused = false;
    }

    public void markCompleted(int index) {
        if (index >= 0 && index < completedStatus.size()) {
            completedStatus.set(index, true);
        }
    }

    public boolean isAllCompleted() {
        for (boolean status : completedStatus) {
            if (!status) return false;
        }
        return !completedStatus.isEmpty();
    }
}
