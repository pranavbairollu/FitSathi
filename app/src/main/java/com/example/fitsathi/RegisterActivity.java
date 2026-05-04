package com.example.fitsathi;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    EditText emailInput, passwordInput, confirmPasswordInput;
    Button registerBtn, goToLoginBtn;
    TextView welcomeText;
    android.widget.ProgressBar progressBar;
    FirebaseAuth mAuth;

    // Regex for the new password policy
    private static final String PASSWORD_PATTERN =
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        welcomeText = findViewById(R.id.welcome_text);
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        confirmPasswordInput = findViewById(R.id.password_confirm_input);
        registerBtn = findViewById(R.id.btn_register);
        goToLoginBtn = findViewById(R.id.btn_goto_login);
        progressBar = findViewById(R.id.register_progress);

        welcomeText.setText("Join FitSathi 👋");

        registerBtn.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();
            String confirmPassword = confirmPasswordInput.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInput.setError("Please enter a valid email address");
                return;
            }

            // Enforce the new password policy
            if (!isValidPassword(password)) {
                passwordInput.setError("Password must be at least 8 characters and include uppercase, lowercase, number, and special character.");
                return;
            }

            if (!password.equals(confirmPassword)) {
                confirmPasswordInput.setError("Passwords do not match");
                return;
            }

            setLoading(true);
            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                setLoading(false);
                if (task.isSuccessful()) {
                    startActivity(new Intent(this, UserInfoActivity.class));
                    finish();
                } else {
                    String error = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                }
            });
        });

        goToLoginBtn.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? android.view.View.VISIBLE : android.view.View.GONE);
        registerBtn.setEnabled(!isLoading);
        goToLoginBtn.setEnabled(!isLoading);
        emailInput.setEnabled(!isLoading);
        passwordInput.setEnabled(!isLoading);
        confirmPasswordInput.setEnabled(!isLoading);
    }

    private boolean isValidPassword(String password) {
        Pattern pattern = Pattern.compile(PASSWORD_PATTERN);
        Matcher matcher = pattern.matcher(password);
        return matcher.matches();
    }
}
