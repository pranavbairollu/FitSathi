package com.example.fitsathi.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitsathi.R;
import com.example.fitsathi.SquadLeaderboardActivity;
import com.example.fitsathi.adapters.SquadAdapter;
import com.example.fitsathi.managers.SquadManager;
import com.example.fitsathi.models.Squad;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class SquadsFragment extends Fragment {

    private RecyclerView rvSquads;
    private SquadAdapter adapter;
    private List<Squad> squadList = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView tvEmptyState;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_squads, container, false);

        rvSquads = view.findViewById(R.id.rv_squads);
        progressBar = view.findViewById(R.id.progress_bar);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);

        rvSquads.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SquadAdapter(squadList, squad -> {
            Intent intent = new Intent(getContext(), SquadLeaderboardActivity.class);
            intent.putExtra("squadId", squad.getId());
            intent.putExtra("squadName", squad.getName());
            intent.putExtra("inviteCode", squad.getInviteCode());
            startActivity(intent);
        }, squad -> {
            showLeaveSquadDialog(squad);
        });

        rvSquads.setAdapter(adapter);

        view.findViewById(R.id.btn_create_squad).setOnClickListener(v -> showCreateSquadDialog());
        view.findViewById(R.id.btn_join_squad).setOnClickListener(v -> showJoinSquadDialog());

        loadSquads();

        return view;
    }

    private void loadSquads() {
        if (progressBar.getVisibility() == View.VISIBLE) return; // Prevent duplicate calls
        
        progressBar.setVisibility(View.VISIBLE);
        SquadManager.getUserSquads((squads, error) -> {
            if (getActivity() == null || !isAdded()) return;
            progressBar.setVisibility(View.GONE);
            if (squads != null) {
                squadList.clear();
                squadList.addAll(squads);
                adapter.notifyDataSetChanged();
                tvEmptyState.setVisibility(squadList.isEmpty() ? View.VISIBLE : View.GONE);
            } else {
                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCreateSquadDialog() {
        EditText input = new EditText(getContext());
        input.setHint("Squad Name (Min 3 chars)");
        input.setPadding(64, 32, 64, 32);
        input.setFilters(new android.text.InputFilter[] { new android.text.InputFilter.LengthFilter(20) });

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Create New Squad")
                .setView(input)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.length() >= 3) {
                        progressBar.setVisibility(View.VISIBLE); // Show loading
                        SquadManager.createSquad(name, (squad, error) -> {
                            if (getActivity() == null || !isAdded()) return;
                            progressBar.setVisibility(View.GONE);
                            if (squad != null) {
                                Toast.makeText(getContext(), "Squad created!", Toast.LENGTH_SHORT).show();
                                loadSquads();
                            } else {
                                Toast.makeText(getContext(), "Failed: " + error, Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        Toast.makeText(getContext(), "Name too short", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showJoinSquadDialog() {
        EditText input = new EditText(getContext());
        input.setHint("6-Character Invite Code");
        input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        input.setPadding(64, 32, 64, 32);
        input.setFilters(new android.text.InputFilter[] { new android.text.InputFilter.LengthFilter(6) });

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Join a Squad")
                .setMessage("Enter the code shared by your friend.")
                .setView(input)
                .setPositiveButton("Join", (dialog, which) -> {
                    String code = input.getText().toString().trim();
                    if (code.length() == 6) {
                        progressBar.setVisibility(View.VISIBLE);
                        SquadManager.joinSquad(code, (squad, error) -> {
                            if (getActivity() == null || !isAdded()) return;
                            progressBar.setVisibility(View.GONE);
                            if (squad != null) {
                                Toast.makeText(getContext(), "Joined " + squad.getName(), Toast.LENGTH_SHORT).show();
                                loadSquads();
                            } else {
                                Toast.makeText(getContext(), "Failed: " + error, Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        Toast.makeText(getContext(), "Invalid code length", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showLeaveSquadDialog(Squad squad) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Leave Squad")
                .setMessage("Are you sure you want to leave '" + squad.getName() + "'?")
                .setPositiveButton("Leave", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);
                    SquadManager.leaveSquad(squad.getId(), squad.getInviteCode(), (success, error) -> {
                        if (getActivity() == null || !isAdded()) return;
                        progressBar.setVisibility(View.GONE);
                        if (success) {
                            Toast.makeText(getContext(), "Left squad", Toast.LENGTH_SHORT).show();
                            loadSquads();
                        } else {
                            Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Stay", null)
                .show();
    }
}
