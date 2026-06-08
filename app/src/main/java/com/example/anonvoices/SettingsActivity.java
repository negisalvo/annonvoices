package com.example.anonvoices;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;

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
}