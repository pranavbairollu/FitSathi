package com.example.fitsathi.fragments;

import android.content.Intent;
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
    private Button btnManagePermissions;
    private ImageView statusIcon;
    private View progressOverlay;

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
        btnManagePermissions = view.findViewById(R.id.btn_manage_permissions);
        statusIcon = view.findViewById(R.id.status_icon);
        progressOverlay = view.findViewById(R.id.progress_overlay);

        checkAvailability(view);

        btnConnect.setOnClickListener(v -> {
            hcManager.checkPermissions(granted -> {
                if (granted) {
                    // Guide user to manage permissions if they want to disconnect
                    openHealthConnectSettings();
                } else {
                    requestPermissionLauncher.launch(HealthConnectManager.PERMISSIONS);
                }
            });
        });

        btnManagePermissions.setOnClickListener(v -> openHealthConnectSettings());

        btnSyncNow.setOnClickListener(v -> {
            hcManager.checkPermissions(granted -> {
                if (granted) {
                    showLoading(true);
                    OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(HealthSyncWorker.class).build();
                    WorkManager.getInstance(requireContext()).enqueue(syncRequest);
                    
                    // Observe work status
                    WorkManager.getInstance(requireContext())
                            .getWorkInfoByIdLiveData(syncRequest.getId())
                            .observe(getViewLifecycleOwner(), workInfo -> {
                                if (workInfo != null && workInfo.getState().isFinished()) {
                                    showLoading(false);
                                    if (workInfo.getState() == androidx.work.WorkInfo.State.SUCCEEDED) {
                                        Toast.makeText(getContext(), "Sync successful!", Toast.LENGTH_SHORT).show();
                                        updateLastSyncTime();
                                    } else {
                                        Toast.makeText(getContext(), "Sync failed. Check permissions.", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                } else {
                    requireActivity().runOnUiThread(() -> Toast.makeText(getContext(), R.string.health_sync_permissions_required, Toast.LENGTH_SHORT).show());
                }
            });
        });

        return view;
    }

    private void openHealthConnectSettings() {
        Intent intent = new Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS");
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Could not open settings", Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoading(boolean loading) {
        if (progressOverlay != null) {
            progressOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        btnSyncNow.setEnabled(!loading);
    }

    private void updateLastSyncTime() {
        String now = java.text.DateFormat.getDateTimeInstance().format(new java.util.Date());
        lastSyncText.setText("Last sync: " + now);
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
            btnManagePermissions.setEnabled(false);
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
                btnConnect.setText("Managed in App");
                btnConnect.setEnabled(false);
                btnManagePermissions.setVisibility(View.VISIBLE);
                statusIcon.setImageResource(android.R.drawable.presence_online);
                btnSyncNow.setEnabled(true);
            } else {
                statusText.setText(R.string.health_sync_status_disconnected);
                statusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                btnConnect.setText(R.string.health_sync_connect);
                btnConnect.setEnabled(true);
                btnManagePermissions.setVisibility(View.GONE);
                statusIcon.setImageResource(android.R.drawable.presence_offline);
                btnSyncNow.setEnabled(false);
            }
        });
    }
}
