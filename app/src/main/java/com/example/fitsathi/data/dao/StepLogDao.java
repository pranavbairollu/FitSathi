package com.example.fitsathi.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.fitsathi.data.entities.StepLog;
import java.util.List;

@Dao
public interface StepLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(StepLog stepLog);

    @Query("SELECT * FROM step_logs WHERE date = :date LIMIT 1")
    StepLog getStepsForDate(String date);

    @Query("SELECT * FROM step_logs WHERE date IN (:dates) ORDER BY date ASC")
    List<StepLog> getStepsForDates(List<String> dates);

    @Query("DELETE FROM step_logs")
    void deleteAll();
}
