package com.example.fitsathi;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fitsathi.managers.UserInfoManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    EditText emailInput, passwordInput;
    Button loginBtn, goToRegisterBtn;
    TextView welcomeText, forgotPasswordText;
    FirebaseAuth mAuth;

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            checkUserInfoAndProceed();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        welcomeText = findViewById(R.id.welcome_text);
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        loginBtn = findViewById(R.id.btn_login);
        goToRegisterBtn = findViewById(R.id.btn_goto_register);
        forgotPasswordText = findViewById(R.id.forgot_password_text);

        welcomeText.setText("Welcome Back 👋");

        loginBtn.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    checkUserInfoAndProceed();
                } else {
                    Toast.makeText(this, "Login failed. Check your credentials.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        goToRegisterBtn.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        forgotPasswordText.setOnClickListener(v -> {
            showForgotPasswordDialog();
        });
    }

    private void checkUserInfoAndProceed() {
        UserInfoManager.getUserInfo(userInfo -> {
            if (userInfo == null) {
                startActivity(new Intent(this, UserInfoActivity.class));
            } else {
                startActivity(new Intent(this, DashboardActivity.class));
            }
            finish();
        });
    }

    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_forgot_password, null);
        builder.setView(dialogView);

        final EditText emailEditText = dialogView.findViewById(R.id.email_input);

        builder.setTitle("Forgot Password")
                .setPositiveButton("Send", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String email = emailEditText.getText().toString().trim();
                        if (TextUtils.isEmpty(email)) {
                            Toast.makeText(LoginActivity.this, "Please enter your email", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        sendPasswordResetEmail(email);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void sendPasswordResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Password reset email sent", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(LoginActivity.this, "Failed to send reset email", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
