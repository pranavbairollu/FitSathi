package com.example.fitsathi.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitsathi.R;
import com.example.fitsathi.models.SquadMemberStat;

import java.util.List;
import java.util.Locale;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    private final List<SquadMemberStat> stats;
    private final boolean isSteps;

    public LeaderboardAdapter(List<SquadMemberStat> stats, boolean isSteps) {
        this.stats = stats;
        this.isSteps = isSteps;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leaderboard, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SquadMemberStat stat = stats.get(position);
        holder.tvRank.setText(String.valueOf(position + 1));
        holder.tvName.setText(stat.getDisplayName());
        
        if (isSteps) {
            holder.tvScore.setText(String.format(Locale.US, "%,d", stat.getSteps()));
        } else {
            holder.tvScore.setText(String.format(Locale.US, "%,.0f kcal", stat.getCalories()));
        }

        // Highlight top 3
        if (position == 0) holder.tvRank.setBackgroundResource(R.drawable.circle_bg_orange); // Gold
        else if (position == 1) holder.tvRank.setBackgroundResource(R.drawable.circle_bg_blue); // Silver
        else if (position == 2) holder.tvRank.setBackgroundResource(R.drawable.circle_bg_purple); // Bronze
        else holder.tvRank.setBackgroundResource(R.drawable.circle_bg_green);
    }

    @Override
    public int getItemCount() {
        return stats.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank, tvName, tvScore;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tv_rank);
            tvName = itemView.findViewById(R.id.tv_user_name);
            tvScore = itemView.findViewById(R.id.tv_score);
        }
    }
}
