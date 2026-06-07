package com.example.anonvoices;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class TeacherDashboardActivity extends AppCompatActivity {

    private TextView tvCountTotal, tvCountUnread;
    private EditText etSearch;
    private RecyclerView recyclerView;
    private FeedbackAdapter adapter;

    private List<Feedback> feedbackList = new ArrayList<>();
    private List<Feedback> filteredList = new ArrayList<>();

    private DatabaseReference feedbackRef;
    private DatabaseReference sessionsRef;
    private Query feedbackQuery;
    private ValueEventListener valueEventListener;

    private String activeFilter = "All";
    private String activeSearchQuery = "";

    // Choice chips
    private TextView chipAll, chipUnread, chipSuggestion, chipComplaint, chipQuestion, chipAppreciation;
    private TextView[] filterChips;

    private FirebaseAuth mAuth;
    private String currentTeacherUid;
    private String currentTeacherName = "Teacher";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_dashboard);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        currentTeacherUid = currentUser.getUid();

        // Fetch Teacher's Name
        FirebaseDatabase.getInstance(AnonVoicesApplication.DATABASE_URL).getReference("teachers").child(currentTeacherUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            currentTeacherName = snapshot.child("name").getValue(String.class);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(TeacherDashboardActivity.this, "Error fetching teacher name: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

        // Initialize header controls
        ImageButton btnToggleTheme = findViewById(R.id.btnToggleTheme);
        ImageButton btnLogout = findViewById(R.id.btnLogout);

        // Theme switching handler
        SharedPreferences themePrefs = getSharedPreferences("ThemePrefs", MODE_PRIVATE);
        boolean isDark = themePrefs.getBoolean("isDarkMode", false);
        btnToggleTheme.setImageResource(isDark ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);

        btnToggleTheme.setOnClickListener(v -> {
            boolean currentMode = themePrefs.getBoolean("isDarkMode", false);
            boolean newMode = !currentMode;
            themePrefs.edit().putBoolean("isDarkMode", newMode).apply();

            if (newMode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
            Toast.makeText(this, newMode ? "Dark Theme Enabled" : "Light Theme Enabled", Toast.LENGTH_SHORT).show();
            recreate();
        });

        // Logout handler
        btnLogout.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Sign Out")
                    .setMessage("Are you sure you want to sign out of the Teacher Portal?")
                    .setPositiveButton("Sign Out", (dialog, which) -> {
                        mAuth.signOut();
                        Toast.makeText(TeacherDashboardActivity.this, "Logged out", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(TeacherDashboardActivity.this, MainActivity.class));
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Initialize counters, search views, and FAB
        tvCountTotal = findViewById(R.id.tvCountTotal);
        tvCountUnread = findViewById(R.id.tvCountUnread);
        etSearch = findViewById(R.id.etSearch);
        recyclerView = findViewById(R.id.recyclerView);
        ExtendedFloatingActionButton fabInitiateSession = findViewById(R.id.fabInitiateSession);

        fabInitiateSession.setOnClickListener(v -> showCreateSessionDialog());

        // Setup filter chips
        chipAll = findViewById(R.id.chipAll);
        chipUnread = findViewById(R.id.chipUnread);
        chipSuggestion = findViewById(R.id.chipSuggestion);
        chipComplaint = findViewById(R.id.chipComplaint);
        chipQuestion = findViewById(R.id.chipQuestion);
        chipAppreciation = findViewById(R.id.chipAppreciation);

        filterChips = new TextView[]{chipAll, chipUnread, chipSuggestion, chipComplaint, chipQuestion, chipAppreciation};

        // Wire click actions to filters
        for (TextView chip : filterChips) {
            chip.setOnClickListener(v -> handleFilterSelection(chip));
        }

        // Configure search watcher
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                activeSearchQuery = s.toString().trim().toLowerCase();
                applyFiltersAndSearch();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Configure RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FeedbackAdapter(filteredList, this::openFeedbackDetailsBottomSheet);
        recyclerView.setAdapter(adapter);

        // Reference to database nodes
        FirebaseDatabase db = FirebaseDatabase.getInstance(AnonVoicesApplication.DATABASE_URL);
        feedbackRef = db.getReference("feedback");
        sessionsRef = db.getReference("sessions");

        // QUERY DESIGN: Restrict retrieved feedback strictly to this teacher's UID
        feedbackQuery = feedbackRef.orderByChild("teacherUid").equalTo(currentTeacherUid);

        loadFeedback();
    }

    private void showCreateSessionDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_session, null);
        TextInputLayout tilTitle = dialogView.findViewById(R.id.tilSessionTitle);
        EditText etTitle = dialogView.findViewById(R.id.etSessionTitle);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelSession);
        MaterialButton btnGenerate = dialogView.findViewById(R.id.btnCreateSessionCode);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnGenerate.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            if (TextUtils.isEmpty(title)) {
                tilTitle.setError("Please enter a feedback topic title");
                return;
            }
            tilTitle.setError(null);
            
            // Show loading state on button
            btnGenerate.setEnabled(false);
            btnGenerate.setText("Connecting...");
            
            // Safety timeout: if no response in 10s
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (dialog.isShowing() && !btnGenerate.isEnabled()) {
                    btnGenerate.setEnabled(true);
                    btnGenerate.setText("Generate Code");
                    Toast.makeText(this, "Connection Timeout. Check Firebase URL/Rules.", Toast.LENGTH_LONG).show();
                }
            }, 10000);

            generateUniqueCodeAndSave(title, dialog);
        });

        dialog.show();
    }

    private void generateUniqueCodeAndSave(String title, AlertDialog createDialog) {
        android.util.Log.d("FirebaseDebug", "Starting session generation for: " + title);
        Random random = new Random();
        // Generate random 6-digit number
        int codeInt = 100000 + random.nextInt(900000);
        String code = String.valueOf(codeInt);

        android.util.Log.d("FirebaseDebug", "Generated code: " + code + ". Checking uniqueness...");

        // Verify uniqueness in sessions node
        sessionsRef.child(code).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                android.util.Log.d("FirebaseDebug", "Uniqueness check result: " + snapshot.exists());
                if (snapshot.exists()) {
                    // Collision (extremely rare), retry generation
                    generateUniqueCodeAndSave(title, createDialog);
                } else {
                    // Save session to Firebase
                    HashMap<String, Object> sessionData = new HashMap<>();
                    sessionData.put("code", code);
                    sessionData.put("title", title);
                    sessionData.put("teacherUid", currentTeacherUid);
                    sessionData.put("teacherName", currentTeacherName);
                    sessionData.put("active", true);
                    sessionData.put("timestamp", System.currentTimeMillis());

                    android.util.Log.d("FirebaseDebug", "Saving session data...");
                    sessionsRef.child(code).setValue(sessionData)
                            .addOnCompleteListener(task -> {
                                android.util.Log.d("FirebaseDebug", "Session save success: " + task.isSuccessful());
                                if (task.isSuccessful()) {
                                    if (createDialog != null && createDialog.isShowing()) {
                                        createDialog.dismiss();
                                    }
                                    showCodeDialog(title, code);
                                } else {
                                    String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                                    android.util.Log.e("FirebaseDebug", "Session save failed: " + error);
                                    Toast.makeText(TeacherDashboardActivity.this, "Failed: " + error, Toast.LENGTH_LONG).show();
                                    // Reset button state
                                    MaterialButton btnGenerate = createDialog.findViewById(R.id.btnCreateSessionCode);
                                    if (btnGenerate != null) {
                                        btnGenerate.setEnabled(true);
                                        btnGenerate.setText("Generate Code");
                                    }
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("FirebaseDebug", "Cancelled: " + error.getMessage());
                Toast.makeText(TeacherDashboardActivity.this, "Database Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                if (error.getCode() == DatabaseError.PERMISSION_DENIED) {
                    Toast.makeText(TeacherDashboardActivity.this, "Check Firebase Rules!", Toast.LENGTH_SHORT).show();
                }
                // Reset button state
                MaterialButton btnGenerate = createDialog.findViewById(R.id.btnCreateSessionCode);
                if (btnGenerate != null) {
                    btnGenerate.setEnabled(true);
                    btnGenerate.setText("Generate Code");
                }
            }
        });
    }

    private void showCodeDialog(String title, String code) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_show_code, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvShowSessionTitle);
        TextView tvCode = dialogView.findViewById(R.id.tvShowSessionCode);
        ImageView ivQR = dialogView.findViewById(R.id.ivShowSessionQR);
        MaterialButton btnDismiss = dialogView.findViewById(R.id.btnDismissShowCode);

        // Add a space in the middle of code for readability (e.g. 483 920)
        String formattedCode = code.substring(0, 3) + " " + code.substring(3);

        tvTitle.setText(title);
        tvCode.setText(formattedCode);

        // Generate and set QR code
        Bitmap qrBitmap = QRUtils.generateQRCode(code);
        if (qrBitmap != null) {
            ivQR.setImageBitmap(qrBitmap);
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnDismiss.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void handleFilterSelection(TextView selectedChip) {
        // Reset all chips visual state
        for (TextView chip : filterChips) {
            chip.setBackgroundResource(R.drawable.chip_unselected);
            chip.setTextColor(ContextCompat.getColor(this, R.color.gray_600));
        }

        // Highlight selected chip
        selectedChip.setBackgroundResource(R.drawable.chip_selected);
        selectedChip.setTextColor(ContextCompat.getColor(this, R.color.white));

        // Update active filter and reapply
        activeFilter = selectedChip.getText().toString();
        applyFiltersAndSearch();
    }

    private void loadFeedback() {
        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                feedbackList.clear();
                int totalCount = 0;
                int unreadCount = 0;

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Feedback f = ds.getValue(Feedback.class);
                    if (f != null) {
                        feedbackList.add(f);
                        totalCount++;
                        if (!f.isRead()) {
                            unreadCount++;
                        }
                    }
                }

                // Sort: Newest submissions first
                Collections.sort(feedbackList, (f1, f2) -> Long.compare(f2.getTimestamp(), f1.getTimestamp()));

                // Update dynamic counts
                tvCountTotal.setText(String.valueOf(totalCount));
                tvCountUnread.setText(String.valueOf(unreadCount));

                applyFiltersAndSearch();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TeacherDashboardActivity.this, "Failed to load voices: " + error.getMessage(), Toast.LENGTH_LONG).show();
                android.util.Log.e("FirebaseError", "Database Error: " + error.getMessage() + " Details: " + error.getDetails());
            }
        };
        feedbackQuery.addValueEventListener(valueEventListener);
    }

    private void applyFiltersAndSearch() {
        filteredList.clear();

        for (Feedback f : feedbackList) {
            // 1. Apply category / read filtering
            boolean matchesFilter = false;
            if (activeFilter.equals("All")) {
                matchesFilter = true;
            } else if (activeFilter.equals("Unread")) {
                matchesFilter = !f.isRead();
            } else {
                matchesFilter = (f.getCategory() != null && f.getCategory().equalsIgnoreCase(activeFilter));
            }

            // 2. Apply search queries (searching topic or feedback content)
            boolean matchesSearch = true;
            if (!activeSearchQuery.isEmpty()) {
                String message = f.getMessage() != null ? f.getMessage().toLowerCase() : "";
                String title = f.getSessionTitle() != null ? f.getSessionTitle().toLowerCase() : "";
                String code = f.getSessionCode() != null ? f.getSessionCode() : "";
                matchesSearch = message.contains(activeSearchQuery) || title.contains(activeSearchQuery) || code.contains(activeSearchQuery);
            }

            if (matchesFilter && matchesSearch) {
                filteredList.add(f);
            }
        }

        adapter.notifyDataSetChanged();
    }

    private void openFeedbackDetailsBottomSheet(Feedback feedback) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_feedback_detail, null);

        TextView tvCategory = sheetView.findViewById(R.id.tvDetailCategory);
        TextView tvDate = sheetView.findViewById(R.id.tvDetailDate);
        TextView tvMessage = sheetView.findViewById(R.id.tvDetailMessage);
        MaterialButton btnMarkRead = sheetView.findViewById(R.id.btnDetailMarkRead);
        MaterialButton btnCopy = sheetView.findViewById(R.id.btnDetailCopy);
        MaterialButton btnDelete = sheetView.findViewById(R.id.btnDetailDelete);

        // Bind data
        String detailHeader = feedback.getCategory() + " • Session " + feedback.getSessionCode();
        tvCategory.setText(detailHeader);
        tvMessage.setText(feedback.getMessage());

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());
        tvDate.setText(sdf.format(new Date(feedback.getTimestamp())));

        // Style category tag based on type
        int colorAccent;
        String category = feedback.getCategory() != null ? feedback.getCategory() : "Suggestion";
        switch (category) {
            case "Complaint":
                colorAccent = ContextCompat.getColor(this, R.color.cat_complaint);
                break;
            case "Question":
                colorAccent = ContextCompat.getColor(this, R.color.cat_question);
                break;
            case "Appreciation":
                colorAccent = ContextCompat.getColor(this, R.color.cat_appreciation);
                break;
            case "Suggestion":
            default:
                colorAccent = ContextCompat.getColor(this, R.color.cat_suggestion);
                break;
        }
        tvCategory.setTextColor(colorAccent);
        tvCategory.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colorAccent & 0x15FFFFFF | 0x1A000000));

        // Mark as read button state
        if (feedback.isRead()) {
            btnMarkRead.setEnabled(false);
            btnMarkRead.setText("Already Read");
            btnMarkRead.setBackgroundColor(ContextCompat.getColor(this, R.color.gray_400));
        } else {
            btnMarkRead.setEnabled(true);
            btnMarkRead.setText("Mark as Read");
            btnMarkRead.setOnClickListener(v -> {
                markAsRead(feedback.getId());
                bottomSheetDialog.dismiss();
            });
        }

        // Copy button action
        btnCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Anonymous Feedback", feedback.getMessage());
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });

        // Delete button action
        btnDelete.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Delete Voice")
                    .setMessage("Are you sure you want to delete this feedback forever?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        deleteFeedback(feedback.getId());
                        bottomSheetDialog.dismiss();
                        Toast.makeText(TeacherDashboardActivity.this, "Feedback deleted", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Automatically mark as read when sheet opens
        if (!feedback.isRead()) {
            markAsRead(feedback.getId());
        }

        bottomSheetDialog.setContentView(sheetView);
        bottomSheetDialog.show();
    }

    private void markAsRead(String id) {
        if (id != null) {
            feedbackRef.child(id).child("read").setValue(true);
        }
    }

    private void deleteFeedback(String id) {
        if (id != null) {
            feedbackRef.child(id).removeValue();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (feedbackQuery != null && valueEventListener != null) {
            feedbackQuery.removeEventListener(valueEventListener);
        }
    }
}
