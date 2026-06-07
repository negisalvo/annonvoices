package com.example.anonvoices;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

public class JoinSessionActivity extends AppCompatActivity {

    private TextInputLayout tilCode;
    private EditText etCode;
    private MaterialButton btnJoin, btnScan;
    private ImageButton btnBack;
    private DatabaseReference sessionsRef;

    private final androidx.activity.result.ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if(result.getContents() != null) {
                    etCode.setText(result.getContents());
                    validateAndJoin(result.getContents());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_session);

        tilCode = findViewById(R.id.tilSessionCode);
        etCode = findViewById(R.id.etSessionCode);
        btnJoin = findViewById(R.id.btnJoin);
        btnScan = findViewById(R.id.btnScanQR);
        btnBack = findViewById(R.id.btnBack);

        // Ensure student is signed in anonymously to satisfy database rules
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            FirebaseAuth.getInstance().signInAnonymously()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Connected Anonymously", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Connection failed. Please check internet.", Toast.LENGTH_LONG).show();
                        }
                    });
        }

        sessionsRef = FirebaseDatabase.getInstance(AnonVoicesApplication.DATABASE_URL).getReference("sessions");

        btnBack.setOnClickListener(v -> finish());

        btnJoin.setOnClickListener(v -> {
            String code = etCode.getText().toString().trim();
            if (code.length() != 6) {
                tilCode.setError("Please enter a valid 6-digit code");
                return;
            }
            tilCode.setError(null);
            validateAndJoin(code);
        });

        btnScan.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
            options.setPrompt("Scan a session QR code");
            options.setCameraId(0);
            options.setBeepEnabled(false);
            options.setBarcodeImageEnabled(true);
            options.setOrientationLocked(false);
            barcodeLauncher.launch(options);
        });
    }

    private void validateAndJoin(String code) {
        btnJoin.setEnabled(false);
        btnJoin.setText("Verifying...");

        sessionsRef.child(code).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.child("active").getValue(Boolean.class) == Boolean.TRUE) {
                    String title = snapshot.child("title").getValue(String.class);
                    String teacherUid = snapshot.child("teacherUid").getValue(String.class);
                    String teacherName = snapshot.child("teacherName").getValue(String.class);
                    
                    Intent intent = new Intent(JoinSessionActivity.this, StudentActivity.class);
                    intent.putExtra("sessionCode", code);
                    intent.putExtra("sessionTitle", title);
                    intent.putExtra("teacherUid", teacherUid);
                    intent.putExtra("teacherName", teacherName);
                    startActivity(intent);
                    finish();
                } else {
                    btnJoin.setEnabled(true);
                    btnJoin.setText("Join Session");
                    tilCode.setError("Invalid or inactive session code");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                btnJoin.setEnabled(true);
                btnJoin.setText("Join Session");
                Toast.makeText(JoinSessionActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
