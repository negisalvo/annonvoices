package com.example.anonvoices;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CardView cardStudent = findViewById(R.id.cardStudent);
        CardView cardTeacher = findViewById(R.id.cardTeacher);

        cardStudent.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, JoinSessionActivity.class));
        });

        cardTeacher.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, TeacherLoginActivity.class));
        });
    }
}
