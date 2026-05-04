package com.example.fitsathi.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fitsathi.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class WaterLogAdapter extends RecyclerView.Adapter<WaterLogAdapter.LogViewHolder> {

    private final List<Long> logTimestamps = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    public WaterLogAdapter(List<Long> initialTimestamps) {
        updateLog(initialTimestamps);
    }

    public void updateLog(List<Long> newTimestamps) {
        logTimestamps.clear();
        if (newTimestamps != null) {
            List<Long> reversedList = new ArrayList<>(newTimestamps);
            Collections.reverse(reversedList);
            logTimestamps.addAll(reversedList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_water_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        long timestamp = logTimestamps.get(position);

        int glassNumber = logTimestamps.size() - position;

        holder.logInfo.setText("Glass " + glassNumber);
        holder.logTime.setText(timeFormat.format(timestamp));
    }

    @Override
    public int getItemCount() {
        return logTimestamps.size();
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView logInfo;
        TextView logTime;

        LogViewHolder(@NonNull View itemView) {
            super(itemView);
            logInfo = itemView.findViewById(R.id.tv_log_info);
            logTime = itemView.findViewById(R.id.tv_log_time);
        }
    }
}
