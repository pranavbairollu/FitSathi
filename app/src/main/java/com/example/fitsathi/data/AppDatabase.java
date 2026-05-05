package com.example.fitsathi.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import com.example.fitsathi.data.dao.StepLogDao;
import com.example.fitsathi.data.dao.WaterLogDao;
import com.example.fitsathi.data.dao.WorkoutLogDao;
import com.example.fitsathi.data.entities.StepLog;
import com.example.fitsathi.data.entities.WaterLog;
import com.example.fitsathi.data.entities.WorkoutLog;

@Database(entities = {StepLog.class, WaterLog.class, WorkoutLog.class}, version = 1, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract StepLogDao stepLogDao();
    public abstract WaterLogDao waterLogDao();
    public abstract WorkoutLogDao workoutLogDao();

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "fitsathi_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
