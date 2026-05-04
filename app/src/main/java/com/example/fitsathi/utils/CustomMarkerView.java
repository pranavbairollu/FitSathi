package com.example.fitsathi.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.TextView;

import com.example.fitsathi.R;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

import java.util.Locale;

@SuppressLint("ViewConstructor")
public class CustomMarkerView extends MarkerView {
    private final TextView tvContent;

    public CustomMarkerView(Context context, int layoutResource) {
        super(context, layoutResource);
        tvContent = findViewById(R.id.tvContent);
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        // Display the step count as a whole number
        String text = String.format(Locale.getDefault(), "%,d steps", (int) e.getY());
        tvContent.setText(text);
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        // Center the marker horizontally and place it above the bar
        return new MPPointF(-(getWidth() / 2f), -getHeight() - 10f);
    }
}
        