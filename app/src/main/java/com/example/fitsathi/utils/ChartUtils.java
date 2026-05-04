package com.example.fitsathi.utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ChartUtils {

    public static List<String> getLast7DaysLabels() {
        List<String> labels = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("EEE", Locale.getDefault()); // Mon, Tue...

        cal.add(Calendar.DAY_OF_YEAR, -6); // Start 6 days ago

        for (int i = 0; i < 7; i++) {
            if (i == 6) {
                // The last one (today) → show as "Today"
                labels.add("Today");
            } else {
                labels.add(sdf.format(cal.getTime()));
            }
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        return labels;
    }
}
