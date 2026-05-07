package com.example.fitsathi;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fitsathi.adapters.LeaderboardAdapter;
import com.example.fitsathi.managers.SquadManager;
import com.example.fitsathi.models.Squad;
import com.example.fitsathi.models.SquadMemberStat;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SquadLeaderboardActivity extends BaseActivity {

    private String squadId;
    private String squadName;
    private String inviteCode;
    private RecyclerView rvLeaderboard;
    private ProgressBar progressBar;
    private List<SquadMemberStat> statsList = new ArrayList<>();
    private boolean showingSteps = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_squad_leaderboard);

        squadId = getIntent().getStringExtra("squadId");
        squadName = getIntent().getStringExtra("squadName");

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(squadName);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.inflateMenu(R.menu.menu_squad_leaderboard);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_share_code) {
                shareInviteCode();
                return true;
            }
            return false;
        });

        rvLeaderboard = findViewById(R.id.rv_leaderboard);
        progressBar = findViewById(R.id.progress_bar);
        TabLayout tabLayout = findViewById(R.id.tab_layout);

        rvLeaderboard.setLayoutManager(new LinearLayoutManager(this));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showingSteps = tab.getPosition() == 0;
                updateList();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        loadSquadData();
    }

    private void loadSquadData() {
        progressBar.setVisibility(View.VISIBLE);
        SquadManager.getSquadDetails(squadId, (squad, error) -> {
            if (squad != null) {
                inviteCode = squad.getInviteCode();
            }
        });

        SquadManager.getSquadLeaderboard(squadId, (stats, error) -> {
            progressBar.setVisibility(View.GONE);
            if (stats != null) {
                statsList.clear();
                statsList.addAll(stats);
                updateList();
            } else {
                Toast.makeText(this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateList() {
        if (showingSteps) {
            Collections.sort(statsList, (a, b) -> b.getSteps() - a.getSteps());
        } else {
            Collections.sort(statsList, (a, b) -> Float.compare(b.getCalories(), a.getCalories()));
        }
        rvLeaderboard.setAdapter(new LeaderboardAdapter(statsList, showingSteps));
    }

    private void shareInviteCode() {
        if (inviteCode == null) return;
        
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Squad Invite Code", inviteCode);
        clipboard.setPrimaryClip(clip);
        
        Toast.makeText(this, "Invite Code " + inviteCode + " copied to clipboard!", Toast.LENGTH_LONG).show();
    }
}
