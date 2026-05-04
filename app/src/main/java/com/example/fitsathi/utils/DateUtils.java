package com.example.fitsathi.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtils {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    // Format date as "yyyy-MM-dd" (for SharedPreferences keys)
    public static String getTodayDate() {
        return sdf.format(new Date());
    }

    // DEFINITIVE FIX: New method for food logs to ensure synchronization
    public static String getTodayDateUTC_forFoodLogs() {
        SimpleDateFormat sdfUTC = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        sdfUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdfUTC.format(new Date());
    }

    // Format any Date object as "yyyy-MM-dd"
    public static String formatDate(Date date) {
        return sdf.format(date);
    }

    // Format any Date object as short label: "Mon 20"
    public static String formatShortLabel(Date date) {
        SimpleDateFormat sdfShort = new SimpleDateFormat("EEE dd", Locale.getDefault());
        return sdfShort.format(date);
    }

    // Check if a given date string is yesterday compared to today
    public static boolean isYesterday(String oldDateStr, String todayStr) {
        try {
            Date oldDate = sdf.parse(oldDateStr);
            Date todayDate = sdf.parse(todayStr);

            Calendar cal = Calendar.getInstance();
            cal.setTime(todayDate);
            cal.add(Calendar.DATE, -1); // yesterday

            return sdf.format(cal.getTime()).equals(sdf.format(oldDate));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
