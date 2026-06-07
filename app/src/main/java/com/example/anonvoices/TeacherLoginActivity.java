package com.example.anonvoices;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class TeacherLoginActivity extends AppCompatActivity {

    private TextInputLayout tilName, tilEmail, tilPassword, tilConfirmPassword;
    private EditText etName, etEmail, etPassword, etConfirmPassword;
    private Button btnLogin;
    private TextView tvTitle, tvSubtitle, tvToggleMode;
    private ImageButton btnBack;

    private FirebaseAuth mAuth;
    private boolean isLoginMode = true; // State toggle: Login or Sign Up

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Check if Teacher is already logged in (Persistent Session)
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(TeacherLoginActivity.this, TeacherDashboardActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_teacher_login);

        // Initialize views
        tilName = findViewById(R.id.tilName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        
        btnLogin = findViewById(R.id.btnLogin);
        tvTitle = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvToggleMode = findViewById(R.id.tvToggleMode);
        btnBack = findViewById(R.id.btnBack);

        // Back button navigation
        btnBack.setOnClickListener(v -> finish());

        // Toggle Sign In vs. Sign Up mode
        tvToggleMode.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            clearErrors();
            if (isLoginMode) {
                tvTitle.setText(R.string.teacher_portal);
                tvSubtitle.setText(R.string.teacher_portal_subtitle);
                tilName.setVisibility(View.GONE);
                tilConfirmPassword.setVisibility(View.GONE);
                btnLogin.setText(R.string.sign_in);
                tvToggleMode.setText(R.string.no_account_sign_up);
            } else {
                tvTitle.setText(R.string.create_account);
                tvSubtitle.setText(R.string.create_account_subtitle);
                tilName.setVisibility(View.VISIBLE);
                tilConfirmPassword.setVisibility(View.VISIBLE);
                btnLogin.setText(R.string.register);
                tvToggleMode.setText(R.string.already_account_sign_in);
            }
        });

        // Submit action
        btnLogin.setOnClickListener(v -> {
            if (isLoginMode) {
                handleLogin();
            } else {
                handleSignUp();
            }
        });
    }

    private void handleLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!validateEmail(email) || !validatePassword(password)) {
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText(R.string.signing_in);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(TeacherLoginActivity.this, getString(R.string.welcome_back), Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(TeacherLoginActivity.this, TeacherDashboardActivity.class));
                        finish();
                    } else {
                        btnLogin.setEnabled(true);
                        btnLogin.setText(R.string.sign_in);
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : getString(R.string.auth_failed);
                        Toast.makeText(TeacherLoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void handleSignUp() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (!validateName(name) || !validateEmail(email) || !validatePassword(password) || !validateConfirmPassword(password, confirmPassword)) {
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText(R.string.creating_account);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        saveTeacherProfileAndContinue(name, email);
                    } else {
                        btnLogin.setEnabled(true);
                        btnLogin.setText(R.string.register);
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : getString(R.string.registration_failed);
                        Toast.makeText(TeacherLoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveTeacherProfileAndContinue(String name, String email) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            HashMap<String, Object> profile = new HashMap<>();
            profile.put("uid", uid);
            profile.put("name", name);
            profile.put("email", email);

            FirebaseDatabase.getInstance(AnonVoicesApplication.DATABASE_URL).getReference("teachers").child(uid).setValue(profile)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(TeacherLoginActivity.this, getString(R.string.registration_successful), Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(TeacherLoginActivity.this, TeacherDashboardActivity.class));
                            finish();
                        } else {
                            btnLogin.setEnabled(true);
                            btnLogin.setText(R.string.register);
                            Toast.makeText(TeacherLoginActivity.this, "Failed to save profile. Try logging in.", Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private boolean validateName(String name) {
        if (TextUtils.isEmpty(name)) {
            tilName.setError(getString(R.string.error_name_required));
            return false;
        }
        tilName.setError(null);
        return true;
    }

    private boolean validateEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError(getString(R.string.error_email_required));
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.error_invalid_email));
            return false;
        }
        tilEmail.setError(null);
        return true;
    }

    private boolean validatePassword(String password) {
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError(getString(R.string.error_password_required));
            return false;
        }
        if (password.length() < 6) {
            tilPassword.setError(getString(R.string.error_password_length));
            return false;
        }
        tilPassword.setError(null);
        return true;
    }

    private boolean validateConfirmPassword(String password, String confirmPassword) {
        if (TextUtils.isEmpty(confirmPassword)) {
            tilConfirmPassword.setError(getString(R.string.error_confirm_password));
            return false;
        }
        if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError(getString(R.string.error_passwords_mismatch));
            return false;
        }
        tilConfirmPassword.setError(null);
        return true;
    }

    private void clearErrors() {
        tilName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
    }
}
