package com.example.fitsathi.data.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "step_logs")
public class StepLog {
    @PrimaryKey
    @NonNull
    private String date; // format: yyyyMMdd
    private int steps;

    public StepLog(@NonNull String date, int steps) {
        this.date = date;
        this.steps = steps;
    }

    @NonNull
    public String getDate() {
        return date;
    }

    public void setDate(@NonNull String date) {
        this.date = date;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }
}
