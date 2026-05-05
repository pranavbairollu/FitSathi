package com.example.fitsathi.data.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.List;

@Entity(tableName = "water_logs")
public class WaterLog {
    @PrimaryKey
    @NonNull
    private String date; // format: yyyyMMdd
    private List<Long> intakeList;

    public WaterLog(@NonNull String date, List<Long> intakeList) {
        this.date = date;
        this.intakeList = intakeList;
    }

    @NonNull
    public String getDate() {
        return date;
    }

    public void setDate(@NonNull String date) {
        this.date = date;
    }

    public List<Long> getIntakeList() {
        return intakeList;
    }

    public void setIntakeList(List<Long> intakeList) {
        this.intakeList = intakeList;
    }
}
