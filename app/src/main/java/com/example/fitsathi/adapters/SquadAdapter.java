package com.example.fitsathi.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitsathi.R;
import com.example.fitsathi.models.Squad;

import java.util.List;

public class SquadAdapter extends RecyclerView.Adapter<SquadAdapter.ViewHolder> {

    private final List<Squad> squads;
    private final OnSquadClickListener listener;
    private final OnSquadLongClickListener longClickListener;

    public interface OnSquadClickListener {
        void onSquadClick(Squad squad);
    }

    public interface OnSquadLongClickListener {
        void onSquadLongClick(Squad squad);
    }

    public SquadAdapter(List<Squad> squads, OnSquadClickListener listener, OnSquadLongClickListener longClickListener) {
        this.squads = squads;
        this.listener = listener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_squad, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Squad squad = squads.get(position);
        holder.tvName.setText(squad.getName());
        holder.tvMemberCount.setText(squad.getMemberCount() + " members");
        holder.itemView.setOnClickListener(v -> listener.onSquadClick(squad));
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onSquadLongClick(squad);
                return true;
            }
            return false;
        });
        holder.btnLeaderboard.setOnClickListener(v -> listener.onSquadClick(squad));
    }

    @Override
    public int getItemCount() {
        return squads.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvMemberCount;
        View btnLeaderboard;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_squad_name);
            tvMemberCount = itemView.findViewById(R.id.tv_member_count);
            btnLeaderboard = itemView.findViewById(R.id.btn_view_leaderboard);
        }
    }
}
