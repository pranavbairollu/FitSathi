package com.example.fitsathi.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.fitsathi.data.entities.WaterLog;
import java.util.List;

@Dao
public interface WaterLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(WaterLog waterLog);

    @Query("SELECT * FROM water_logs WHERE date = :date LIMIT 1")
    WaterLog getWaterLogForDate(String date);

    @Query("DELETE FROM water_logs")
    void deleteAll();
}
