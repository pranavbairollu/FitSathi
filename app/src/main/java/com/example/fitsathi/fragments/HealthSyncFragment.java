package com.example.fitsathi.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.health.connect.client.PermissionController;
import androidx.health.connect.client.HealthConnectClient;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.fitsathi.R;
import com.example.fitsathi.managers.HealthConnectManager;
import com.example.fitsathi.workers.HealthSyncWorker;

import java.util.Set;

/**
 * Fragment to manage Health Connect synchronization settings.
 */
public class HealthSyncFragment extends Fragment {

    private HealthConnectManager hcManager;
    private TextView statusText;
    private TextView lastSyncText;
    private Button btnConnect;
    private Button btnSyncNow;
    private ImageView statusIcon;

    private final ActivityResultLauncher<Set<String>> requestPermissionLauncher =
            registerForActivityResult(PermissionController.createRequestPermissionResultContract(), granted -> {
                if (granted.containsAll(HealthConnectManager.PERMISSIONS)) {
                    updateUI(true);
                    Toast.makeText(getContext(), R.string.health_sync_status_connected, Toast.LENGTH_SHORT).show();
                } else {
                    updateUI(false);
                    Toast.makeText(getContext(), R.string.health_sync_permissions_required, Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_health_sync, container, false);

        hcManager = HealthConnectManager.getInstance(requireContext());

        statusText = view.findViewById(R.id.status_text);
        lastSyncText = view.findViewById(R.id.last_sync_text);
        btnConnect = view.findViewById(R.id.btn_connect);
        btnSyncNow = view.findViewById(R.id.btn_sync_now);
        statusIcon = view.findViewById(R.id.status_icon);

        checkAvailability(view);

        btnConnect.setOnClickListener(v -> {
            hcManager.checkPermissions(granted -> {
                if (granted) {
                    // Already granted
                    requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Already connected", Toast.LENGTH_SHORT).show());
                } else {
                    requestPermissionLauncher.launch(HealthConnectManager.PERMISSIONS);
                }
            });
        });

        btnSyncNow.setOnClickListener(v -> {
            hcManager.checkPermissions(granted -> {
                if (granted) {
                    OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(HealthSyncWorker.class).build();
                    WorkManager.getInstance(requireContext()).enqueue(syncRequest);
                    requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), R.string.health_sync_now, Toast.LENGTH_SHORT).show());
                } else {
                    requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), R.string.health_sync_permissions_required, Toast.LENGTH_SHORT).show());
                }
            });
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        hcManager.checkPermissions(this::updateUI);
    }

    private void checkAvailability(View view) {
        int status = HealthConnectManager.isHealthConnectAvailable(requireContext());
        if (status != HealthConnectClient.SDK_AVAILABLE) {
            view.findViewById(R.id.hc_not_installed_text).setVisibility(View.VISIBLE);
            btnConnect.setEnabled(false);
            btnSyncNow.setEnabled(false);
            if (status == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
                ((TextView)view.findViewById(R.id.hc_not_installed_text)).setText("Update Health Connect to proceed");
            }
        }
    }

    private void updateUI(boolean connected) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            if (connected) {
                statusText.setText(R.string.health_sync_status_connected);
                statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                btnConnect.setText(R.string.health_sync_disconnect);
                statusIcon.setImageResource(android.R.drawable.presence_online);
            } else {
                statusText.setText(R.string.health_sync_status_disconnected);
                statusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                btnConnect.setText(R.string.health_sync_connect);
                statusIcon.setImageResource(android.R.drawable.presence_offline);
            }
        });
    }
}
