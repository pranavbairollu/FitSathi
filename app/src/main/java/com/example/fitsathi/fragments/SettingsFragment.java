
package com.example.fitsathi.fragments;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.fitsathi.HelpAndGuidelinesActivity;
import com.example.fitsathi.MealReminderScheduler;
import com.example.fitsathi.PrivacyPolicyActivity;
import com.example.fitsathi.R;
import com.example.fitsathi.databinding.FragmentSettingsBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private SharedPreferences prefs;
    private static final int POST_NOTIFICATIONS_REQUEST_CODE = 123;
    private String currentMealForPermission;
    private SwitchMaterial currentSwitchForPermission;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        applyFadeInAnimation(view);
        prefs = requireActivity().getSharedPreferences(getString(R.string.settings_prefs_name), Context.MODE_PRIVATE);

        setupDarkModeSwitch();
        setupMealSwitches(); // New, robust method
        setupGeneralSection();
        setupClickListeners();
    }

    private void applyFadeInAnimation(View view) {
        if (getContext() != null) {
            Animation fadeIn = AnimationUtils.loadAnimation(getContext(), R.anim.fade_in);
            view.startAnimation(fadeIn);
        }
    }

    private void setupDarkModeSwitch() {
        boolean isDarkMode = prefs.getBoolean(getString(R.string.dark_mode_enabled_key), false);
        binding.darkModeSwitch.setChecked(isDarkMode);

        // This listener is safe because it doesn't have complex side effects like the reminder logic
        binding.darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) {
                return; // Only react to user taps
            }
            prefs.edit().putBoolean(getString(R.string.dark_mode_enabled_key), isChecked).apply();
            int mode = isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
            AppCompatDelegate.setDefaultNightMode(mode);
        });
    }

    private void setupMealSwitches() {
        String[] mealNames = getResources().getStringArray(R.array.meal_types);
        View[] mealRows = {
                binding.rowBreakfast.getRoot(),
                binding.rowLunch.getRoot(),
                binding.rowDinner.getRoot(),
                binding.rowSnacks.getRoot()
        };

        for (int i = 0; i < mealNames.length; i++) {
            final String meal = mealNames[i];
            View row = mealRows[i];
            final SwitchMaterial sw = row.findViewById(R.id.setting_switch);

            // Set the row's title and icon
            ImageView icon = row.findViewById(R.id.setting_icon);
            TextView title = row.findViewById(R.id.setting_title);
            icon.setImageResource(R.drawable.ic_meal);
            title.setText(meal);

            // Restore the initial UI state. This is safe because no listener is active yet.
            sw.setChecked(MealReminderScheduler.isReminderScheduled(requireContext(), meal));

            // Set an OnClickListener. This is the definitive fix.
            // An OnClickListener ONLY fires on a physical user tap. It is immune to
            // programmatic changes and lifecycle events like theme changes.
            sw.setOnClickListener(v -> {
                boolean isChecked = sw.isChecked();

                if (isChecked) {
                    // User is turning the reminder ON.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                            pickReminderTime(meal, sw);
                        } else {
                            // If permission is denied, the switch will be toggled back in onRequestPermissionsResult.
                            currentMealForPermission = meal;
                            currentSwitchForPermission = sw;
                            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, POST_NOTIFICATIONS_REQUEST_CODE);
                        }
                    } else {
                        pickReminderTime(meal, sw);
                    }
                } else {
                    // User is turning the reminder OFF.
                    MealReminderScheduler.cancelReminder(requireContext(), meal);
                    Toast.makeText(getContext(), getString(R.string.meal_reminder_cancelled_toast, meal), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }


    private void setupGeneralSection() {
        View resetDataRow = binding.rowResetData.getRoot();
        TextView resetDataTitle = resetDataRow.findViewById(R.id.setting_title);
        ImageView resetDataIcon = resetDataRow.findViewById(R.id.setting_icon);
        resetDataTitle.setText(R.string.reset_data);
        resetDataIcon.setImageResource(R.drawable.ic_delete);

        View aboutRow = binding.rowAbout.getRoot();
        TextView aboutTitle = aboutRow.findViewById(R.id.setting_title);
        ImageView aboutIcon = aboutRow.findViewById(R.id.setting_icon);
        aboutTitle.setText(R.string.about);
        aboutIcon.setImageResource(R.drawable.ic_info);

        View privacyPolicyRow = binding.rowPrivacyPolicy.getRoot();
        TextView privacyPolicyTitle = privacyPolicyRow.findViewById(R.id.setting_title);
        ImageView privacyPolicyIcon = privacyPolicyRow.findViewById(R.id.setting_icon);
        privacyPolicyTitle.setText(R.string.privacy_policy);
        privacyPolicyIcon.setImageResource(R.drawable.ic_privacy_policy);

        View helpRow = binding.rowHelp.getRoot();
        TextView helpTitle = helpRow.findViewById(R.id.setting_title);
        ImageView helpIcon = helpRow.findViewById(R.id.setting_icon);
        helpTitle.setText(R.string.help_and_guidelines);
        helpIcon.setImageResource(R.drawable.ic_help_outline);
    }

    private void setupClickListeners() {
        binding.rowResetData.getRoot().setOnClickListener(v -> showResetDataDialog());
        binding.rowAbout.getRoot().setOnClickListener(v -> showAboutDialog());
        binding.rowPrivacyPolicy.getRoot().setOnClickListener(v -> showPrivacyPolicy());
        binding.rowHelp.getRoot().setOnClickListener(v -> showHelpAndGuidelines());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == POST_NOTIFICATIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (currentMealForPermission != null && currentSwitchForPermission != null) {
                    pickReminderTime(currentMealForPermission, currentSwitchForPermission);
                }
            } else {
                Toast.makeText(getContext(), "Notification permission is required to set reminders.", Toast.LENGTH_SHORT).show();
                if (currentSwitchForPermission != null) {
                    currentSwitchForPermission.setChecked(false); // Revert switch state if permission denied
                }
            }
            currentMealForPermission = null;
            currentSwitchForPermission = null;
        }
    }

    private void pickReminderTime(String meal, SwitchMaterial sw) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        int hour = c.get(java.util.Calendar.HOUR_OF_DAY);
        int minute = c.get(java.util.Calendar.MINUTE);

        TimePickerDialog dialog = new TimePickerDialog(getContext(),
                (view, h, m) -> {
                    MealReminderScheduler.scheduleDailyReminder(requireContext(), meal, h, m);
                    sw.setChecked(true); // Ensure switch stays on
                    Toast.makeText(getContext(),
                            getString(R.string.meal_reminder_set_toast, meal, h, m),
                            Toast.LENGTH_SHORT).show();
                },
                hour, minute, true);

        dialog.setOnCancelListener(dialogInterface -> sw.setChecked(false)); // If user cancels time picker, revert
        dialog.show();
    }

    private void showResetDataDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.reset_data_dialog_title)
                .setMessage(R.string.reset_data_dialog_message)
                .setPositiveButton(R.string.reset_button_text, (dialog, which) -> {
                    clearAllSharedPreferences();
                    Toast.makeText(getContext(), R.string.all_app_data_reset_toast, Toast.LENGTH_SHORT).show();
                    Intent i = requireContext().getPackageManager().getLaunchIntentForPackage(requireContext().getPackageName());
                    if (i != null) {
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                        requireActivity().finish();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void clearAllSharedPreferences() {
        String[] prefNames = getResources().getStringArray(R.array.preference_files);
        for (String prefName : prefNames) {
            requireActivity().getSharedPreferences(prefName, Context.MODE_PRIVATE).edit().clear().apply();
        }
    }

    private void showAboutDialog() {
        String version = "";
        try {
            PackageInfo pInfo = requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0);
            version = getString(R.string.version_name_prefix) + " " + pInfo.versionName;
        } catch (Exception e) {
            // ignore
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.about_title)
                .setMessage(getString(R.string.about_message) + "\n" + version)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    private void showPrivacyPolicy() {
        Intent intent = new Intent(requireContext(), PrivacyPolicyActivity.class);
        startActivity(intent);
    }

    private void showHelpAndGuidelines() {
        Intent intent = new Intent(requireContext(), HelpAndGuidelinesActivity.class);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
