package com.example.anonvoices;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

public class StudentActivity extends AppCompatActivity {

    private LinearLayout layoutEnterCode, layoutFeedbackForm;
    private TextInputLayout tilSessionCode, tilMessage;
    private EditText etSessionCode, etMessage;
    private Button btnJoinSession, btnSubmit;
    private TextView tvSessionTitle, tvSessionTeacher;
    private ImageButton btnBack;
    private AutoCompleteTextView autoCompleteCategory;

    // Tracking variables for verified session details
    private String joinedSessionCode = "";
    private String joinedSessionTitle = "";
    private String joinedTeacherUid = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student);

        // Bind State A Layout elements
        layoutEnterCode = findViewById(R.id.layoutEnterCode);
        tilSessionCode = findViewById(R.id.tilSessionCode);
        etSessionCode = findViewById(R.id.etSessionCode);
        btnJoinSession = findViewById(R.id.btnJoinSession);

        // Bind State B Layout elements
        layoutFeedbackForm = findViewById(R.id.layoutFeedbackForm);
        tvSessionTitle = findViewById(R.id.tvSessionTitle);
        tvSessionTeacher = findViewById(R.id.tvSessionTeacher);
        autoCompleteCategory = findViewById(R.id.autoCompleteCategory);
        tilMessage = findViewById(R.id.tilMessage);
        etMessage = findViewById(R.id.etMessage);
        btnSubmit = findViewById(R.id.btnSubmit);

        // Header Navigation elements
        btnBack = findViewById(R.id.btnBack);

        // Sign in anonymously if not already authenticated to satisfy database rules
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            FirebaseAuth.getInstance().signInAnonymously()
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Anonymous connection failed. Submissions may not work.", Toast.LENGTH_LONG).show();
                    });
        }

        // Setup Exposed dropdown menu choices
        String[] categories = {"Suggestion", "Complaint", "Question", "Appreciation"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories);
        autoCompleteCategory.setAdapter(adapter);
        autoCompleteCategory.setText(categories[0], false);

        // Check if we arrived here from JoinSessionActivity with intent extras
        Intent intent = getIntent();
        if (intent.hasExtra("sessionCode")) {
            joinedSessionCode = intent.getStringExtra("sessionCode");
            joinedSessionTitle = intent.getStringExtra("sessionTitle");
            joinedTeacherUid = intent.getStringExtra("teacherUid");
            String teacherName = intent.getStringExtra("teacherName");

            // Skip State A and go straight to Form
            tvSessionTitle.setText(joinedSessionTitle);
            tvSessionTeacher.setText("by " + (teacherName != null ? teacherName : "Teacher"));
            layoutEnterCode.setVisibility(View.GONE);
            layoutFeedbackForm.setVisibility(View.VISIBLE);
        }

        // Nav Back Action
        btnBack.setOnClickListener(v -> {
            if (layoutFeedbackForm.getVisibility() == View.VISIBLE && !intent.hasExtra("sessionCode")) {
                // If in form state and NOT started from JoinSessionActivity, drop back to code state
                layoutFeedbackForm.setVisibility(View.GONE);
                layoutEnterCode.setVisibility(View.VISIBLE);
                etSessionCode.setText("");
                clearFeedbackForm();
            } else {
                finish();
            }
        });

        // Dynamic Session code join click handler
        btnJoinSession.setOnClickListener(v -> handleSessionJoin());

        // Dynamic Feedback Submit click handler
        btnSubmit.setOnClickListener(v -> handleFeedbackSubmit());
    }

    private void handleSessionJoin() {
        String code = etSessionCode.getText().toString().trim();
        if (TextUtils.isEmpty(code) || code.length() < 6) {
            tilSessionCode.setError("Please enter a valid 6-digit session code");
            return;
        }
        tilSessionCode.setError(null);

        btnJoinSession.setEnabled(false);
        btnJoinSession.setText("Verifying Code...");

        // Query database sessions/{code}
        FirebaseDatabase.getInstance(AnonVoicesApplication.DATABASE_URL).getReference("sessions").child(code)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        btnJoinSession.setEnabled(true);
                        btnJoinSession.setText("Join Session");

                        if (snapshot.exists()) {
                            Boolean active = snapshot.child("active").getValue(Boolean.class);
                            if (active != null && !active) {
                                tilSessionCode.setError("This session is no longer active.");
                                return;
                            }

                            // Read details
                            String title = snapshot.child("title").getValue(String.class);
                            String teacherName = snapshot.child("teacherName").getValue(String.class);
                            String teacherUid = snapshot.child("teacherUid").getValue(String.class);

                            // Store session metadata
                            joinedSessionCode = code;
                            joinedSessionTitle = title;
                            joinedTeacherUid = teacherUid;

                            // Update Header views & toggle screens
                            tvSessionTitle.setText(title);
                            tvSessionTeacher.setText("by " + (teacherName != null ? teacherName : "Teacher"));
                            
                            layoutEnterCode.setVisibility(View.GONE);
                            layoutFeedbackForm.setVisibility(View.VISIBLE);
                        } else {
                            tilSessionCode.setError("Incorrect session code. Please try again.");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        btnJoinSession.setEnabled(true);
                        btnJoinSession.setText("Join Session");
                        Toast.makeText(StudentActivity.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleFeedbackSubmit() {
        String message = etMessage.getText().toString().trim();
        String category = autoCompleteCategory.getText().toString();

        if (TextUtils.isEmpty(message)) {
            tilMessage.setError("Please type a message before submitting.");
            return;
        }

        if (message.length() > 500) {
            tilMessage.setError("Feedback cannot exceed 500 characters.");
            return;
        }

        tilMessage.setError(null);
        submitFeedback(message, category);
    }

    private void submitFeedback(String message, String category) {
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Submitting...");

        DatabaseReference ref = FirebaseDatabase.getInstance(AnonVoicesApplication.DATABASE_URL).getReference("feedback");
        String id = ref.push().getKey();

        if (id != null) {
            HashMap<String, Object> data = new HashMap<>();
            data.put("id", id);
            data.put("message", message);
            data.put("category", category);
            data.put("read", false);
            data.put("timestamp", System.currentTimeMillis());
            data.put("sessionCode", joinedSessionCode);
            data.put("sessionTitle", joinedSessionTitle);
            data.put("teacherUid", joinedTeacherUid);

            ref.child(id).setValue(data).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    showSuccessDialog();
                } else {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Submit Feedback");
                    Toast.makeText(StudentActivity.this, "Submission failed. Check connection.", Toast.LENGTH_LONG).show();
                }
            });
        } else {
            btnSubmit.setEnabled(true);
            btnSubmit.setText("Submit Feedback");
        }
    }

    private void showSuccessDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_success, null);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialog.show();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isFinishing()) {
                dialog.dismiss();
                finish();
            }
        }, 1800);
    }

    private void clearFeedbackForm() {
        etMessage.setText("");
        tilMessage.setError(null);
    }
}
