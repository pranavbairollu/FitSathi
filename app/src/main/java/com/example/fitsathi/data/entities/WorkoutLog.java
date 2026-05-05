package com.example.fitsathi.data.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "workout_logs")
public class WorkoutLog {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String exerciseName;
    private int caloriesBurned;
    private long timestamp;
    private String category;

    public WorkoutLog(String exerciseName, int caloriesBurned, long timestamp, String category) {
        this.exerciseName = exerciseName;
        this.caloriesBurned = caloriesBurned;
        this.timestamp = timestamp;
        this.category = category;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getExerciseName() {
        return exerciseName;
    }

    public void setExerciseName(String exerciseName) {
        this.exerciseName = exerciseName;
    }

    public int getCaloriesBurned() {
        return caloriesBurned;
    }

    public void setCaloriesBurned(int caloriesBurned) {
        this.caloriesBurned = caloriesBurned;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
