package com.example.fitsathi.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.fitsathi.DashboardActivity;
import com.example.fitsathi.LoginActivity;
import com.example.fitsathi.R;
import com.example.fitsathi.databinding.FragmentProfileBinding;
import com.example.fitsathi.managers.UserInfoManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.yalantis.ucrop.UCrop;

import java.io.File;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private FirebaseAuth mAuth;
    private StorageReference mStorageRef;

    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<Intent> cropImageLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference("profile_pictures");

        registerActivityLaunchers();
        loadUserInfo();
        setupClickListeners();
    }

    private void registerActivityLaunchers() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        startCrop(uri);
                    }
                }
        );

        cropImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (getActivity() == null) return;
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            Uri resultUri = UCrop.getOutput(data);
                            if (resultUri != null) {
                                uploadFile(resultUri);
                            }
                        }
                    } else if (result.getResultCode() == UCrop.RESULT_ERROR) {
                        Throwable cropError = UCrop.getError(result.getData());
                        if (cropError != null) {
                            Toast.makeText(getContext(), "Crop error: " + cropError.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private void loadUserInfo() {
        if (!isAdded()) return;

        UserInfoManager.getUserInfo(userInfo -> {
            if (isAdded() && userInfo != null) {
                binding.profileName.setText(userInfo.getName());
                binding.profileEmail.setText(UserInfoManager.getEmail());

                String profilePicUrl = userInfo.getProfilePicUrl();
                if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                    Glide.with(this).load(profilePicUrl).circleCrop().into(binding.profilePic);
                } else {
                    binding.profilePic.setImageResource(R.drawable.ic_profile); 
                }

                populateInfoRow(binding.ageInfo.getRoot(), R.drawable.ic_cake, "Age", userInfo.getAge() + " years");
                populateInfoRow(binding.heightInfo.getRoot(), R.drawable.ic_height, "Height", userInfo.getHeight() + " cm");
                populateInfoRow(binding.weightInfo.getRoot(), R.drawable.ic_weight, "Weight", userInfo.getWeight() + " kg");
                populateInfoRow(binding.genderInfo.getRoot(), R.drawable.ic_gender, "Gender", userInfo.getGender());
                populateInfoRow(binding.activityInfo.getRoot(), R.drawable.ic_directions_run, "Activity Level", userInfo.getActivityLevel());
            }
        });
    }

    private void populateInfoRow(View rowView, int iconResId, String labelText, String valueText) {
        if (rowView == null) return;
        ImageView icon = rowView.findViewById(R.id.info_icon);
        TextView label = rowView.findViewById(R.id.info_label);
        TextView value = rowView.findViewById(R.id.info_value);
        icon.setImageResource(iconResId);
        label.setText(labelText);
        value.setText(valueText);
    }

    private void setupClickListeners() {
        binding.editProfileButton.setOnClickListener(v -> {
            if (getActivity() instanceof DashboardActivity) {
                ((DashboardActivity) getActivity()).openUserInfoActivity();
            }
        });

        binding.editProfilePicFab.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        binding.shareProgressButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), com.example.fitsathi.ProgressShareActivity.class);
            startActivity(intent);
        });

        binding.logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });
    }

    private void startCrop(@NonNull Uri sourceUri) {
        if (getContext() == null) return;
        String destinationFileName = "cropped_" + System.currentTimeMillis() + ".jpg";
        UCrop uCrop = UCrop.of(sourceUri, Uri.fromFile(new File(getContext().getCacheDir(), destinationFileName)));
        uCrop.withAspectRatio(1, 1);
        uCrop.withMaxResultSize(512, 512);
        cropImageLauncher.launch(uCrop.getIntent(getContext()));
    }

    private void uploadFile(Uri imageUri) {
        if (imageUri != null && mAuth.getCurrentUser() != null) {
            final StorageReference fileReference = mStorageRef.child(mAuth.getCurrentUser().getUid() + "/profile.jpg");

            fileReference.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();
                            UserInfoManager.setProfilePicUrl(imageUrl);
                            if(isAdded()){
                                Glide.with(ProfileFragment.this).load(imageUrl).circleCrop().into(binding.profilePic);
                                Toast.makeText(getContext(), "Upload successful", Toast.LENGTH_SHORT).show();
                            }
                        }).addOnFailureListener(e -> {
                            if(isAdded()){
                                Toast.makeText(getContext(), "Failed to get download URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .addOnFailureListener(e -> {
                        if(isAdded()){
                            Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
