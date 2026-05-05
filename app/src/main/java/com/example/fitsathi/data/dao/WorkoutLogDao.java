package com.example.fitsathi.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.example.fitsathi.data.entities.WorkoutLog;
import java.util.List;

@Dao
public interface WorkoutLogDao {
    @Insert
    void insert(WorkoutLog workoutLog);

    @Query("SELECT * FROM workout_logs ORDER BY timestamp DESC")
    List<WorkoutLog> getAllHistory();

    @Query("SELECT * FROM workout_logs ORDER BY timestamp DESC LIMIT :limit")
    List<WorkoutLog> getRecentHistory(int limit);

    @Query("DELETE FROM workout_logs")
    void deleteAll();
}
