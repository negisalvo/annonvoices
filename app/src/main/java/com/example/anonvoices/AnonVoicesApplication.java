package com.example.anonvoices;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.FirebaseDatabase;

public class AnonVoicesApplication extends Application {

    public static final String DATABASE_URL = "https://apps-67f5a-default-rtdb.firebaseio.com/";

    @Override
    public void onCreate() {
        super.onCreate();

        // Explicitly initialize Firebase if URL is missing from json
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseOptions options = new FirebaseOptions.Builder()
                        .setApplicationId("1:362554722478:android:d3d9b0fad0ab50f0dbc53f")
                        .setApiKey("AIzaSyDp4tTy-sW7z1KcCC0jbEiqbl8Nq_Uvm3g")
                        .setDatabaseUrl(DATABASE_URL)
                        .setProjectId("apps-67f5a")
                        .setStorageBucket("apps-67f5a.firebasestorage.app")
                        .build();
                FirebaseApp.initializeApp(this, options);
            }
            
            FirebaseDatabase.getInstance(DATABASE_URL).setPersistenceEnabled(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Initialize user-preferred theme on app startup
        SharedPreferences sp = getSharedPreferences("ThemePrefs", MODE_PRIVATE);
        boolean isDarkMode = sp.getBoolean("isDarkMode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}
