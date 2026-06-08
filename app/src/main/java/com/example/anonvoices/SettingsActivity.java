package com.example.anonvoices;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // UI References
        ImageButton btnBack = findViewById(R.id.btnBack);
        TextView tvUserName = findViewById(R.id.tvUserName);
        SwitchMaterial switchDarkMode = findViewById(R.id.switchDarkMode);
        LinearLayout rowAbout = findViewById(R.id.rowAbout);
        LinearLayout rowChangePassword = findViewById(R.id.rowChangePassword);
        LinearLayout rowLogout = findViewById(R.id.rowLogout);

        // Set User Name from Intent
        String teacherName = getIntent().getStringExtra("teacherName");
        if (teacherName != null) {
            tvUserName.setText(teacherName);
        }

        // Theme Prefs
        SharedPreferences themePrefs = getSharedPreferences("ThemePrefs", MODE_PRIVATE);
        boolean isDarkMode = themePrefs.getBoolean("isDarkMode", false);
        switchDarkMode.setChecked(isDarkMode);

        btnBack.setOnClickListener(v -> finish());

        // Toggle Dark Mode
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            themePrefs.edit().putBoolean("isDarkMode", isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(isChecked ? 
                    AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            
            Toast.makeText(this, isChecked ? getString(R.string.dark_mode_enabled) : getString(R.string.light_mode_enabled), Toast.LENGTH_SHORT).show();
        });

        // About Dialog
        rowAbout.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.about_anonvoices)
                .setMessage(R.string.about_desc)
                .setPositiveButton(android.R.string.ok, null)
                .show());

        // Change Password
        rowChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        // Logout
        rowLogout.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.sign_out)
                .setMessage(R.string.sign_out_message)
                .setPositiveButton(R.string.sign_out, (dialog, which) -> {
                    // 1. Sign out from Firebase
                    FirebaseAuth.getInstance().signOut();

                    // 2. Redirect and clear back-stack
                    Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                    
                    Toast.makeText(this, R.string.logged_out, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show());
    }

    private void showChangePasswordDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null);
        TextInputLayout tilNew = dialogView.findViewById(R.id.tilNewPassword);
        TextInputLayout tilConfirm = dialogView.findViewById(R.id.tilConfirmNewPassword);
        
        EditText etCurrent = dialogView.findViewById(R.id.etCurrentPassword);
        EditText etNew = dialogView.findViewById(R.id.etNewPassword);
        EditText etConfirm = dialogView.findViewById(R.id.etConfirmNewPassword);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setPositiveButton(R.string.update, null) // Set null to override behavior
                .setNegativeButton(R.string.cancel, null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String currentPw = etCurrent.getText().toString().trim();
                String newPw = etNew.getText().toString().trim();
                String confirmPw = etConfirm.getText().toString().trim();

                if (TextUtils.isEmpty(currentPw) || TextUtils.isEmpty(newPw) || TextUtils.isEmpty(confirmPw)) {
                    Toast.makeText(this, R.string.error_fields_empty, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (newPw.length() < 6) {
                    tilNew.setError(getString(R.string.error_password_length));
                    return;
                } else {
                    tilNew.setError(null);
                }

                if (!newPw.equals(confirmPw)) {
                    tilConfirm.setError(getString(R.string.error_password_match));
                    return;
                } else {
                    tilConfirm.setError(null);
                }

                performPasswordUpdate(currentPw, newPw, dialog);
            });
        });

        dialog.show();
    }

    private void performPasswordUpdate(String currentPw, String newPw, AlertDialog dialog) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPw);

        user.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                user.updatePassword(newPw).addOnCompleteListener(updateTask -> {
                    if (updateTask.isSuccessful()) {
                        Toast.makeText(this, R.string.password_updated, Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                    } else {
                        String error = updateTask.getException() != null ? updateTask.getException().getMessage() : "Update failed";
                        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                Toast.makeText(this, R.string.reauth_failed, Toast.LENGTH_LONG).show();
            }
        });
    }
}