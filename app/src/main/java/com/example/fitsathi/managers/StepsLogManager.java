package com.example.fitsathi.managers;

import android.content.Context;
import com.example.fitsathi.data.AppDatabase;
import com.example.fitsathi.data.entities.StepLog;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;

/**
 * Utility class for saving and retrieving step counts using Room Database.
 * Replaces the legacy SharedPreferences implementation for better scalability.
 */
public class StepsLogManager {
    private static final String DATE_FORMAT = "yyyyMMdd";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface StepsCallback {
        void onStepsReceived(int steps);
    }

    public interface HistoryCallback {
        void onHistoryReceived(List<Integer> stepsList);
    }

    /** Save steps for a given date (Async) */
    public static void saveStepsForDate(Context context, String dateKey, int steps) {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(context);
            db.stepLogDao().insertOrUpdate(new StepLog(dateKey, steps));
        });
    }

    /** Save steps for today (Async) */
    public static void saveTodaySteps(Context context, int steps) {
        saveStepsForDate(context, getTodayKey(), steps);
    }

    /** Get steps for a given date (Async) */
    public static void getStepsForDate(Context context, String dateKey, StepsCallback callback) {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(context);
            StepLog log = db.stepLogDao().getStepsForDate(dateKey);
            int steps = (log != null) ? log.getSteps() : 0;
            if (callback != null) {
                mainHandler.post(() -> callback.onStepsReceived(steps));
            }
        });
    }

    /** Get last 7 days of steps (Async) */
    public static void getLast7DaysSteps(Context context, HistoryCallback callback) {
        executor.execute(() -> {
            List<String> dates = new ArrayList<>();
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -6);
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());

            for (int i = 0; i < 7; i++) {
                dates.add(sdf.format(cal.getTime()));
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }

            AppDatabase db = AppDatabase.getDatabase(context);
            List<StepLog> logs = db.stepLogDao().getStepsForDates(dates);
            
            // Map logs back to a full 7-day list (handling missing days)
            List<Integer> result = new ArrayList<>();
            for (String date : dates) {
                int steps = 0;
                for (StepLog log : logs) {
                    if (log.getDate().equals(date)) {
                        steps = log.getSteps();
                        break;
                    }
                }
                result.add(steps);
            }

            if (callback != null) {
                mainHandler.post(() -> callback.onHistoryReceived(result));
            }
        });
    }

    /** Helper: returns today's key (yyyyMMdd) */
    private static String getTodayKey() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        return sdf.format(cal.getTime());
    }
}
