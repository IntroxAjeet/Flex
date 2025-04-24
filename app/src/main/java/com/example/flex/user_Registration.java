package com.example.flex;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class user_Registration extends AppCompatActivity {

    private Button button;
    private EditText user_name,user_password,user_email,password;
    private TextView login;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private FirebaseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_registration);

        database = FirebaseDatabase.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("user");
        mAuth = FirebaseAuth.getInstance();

        button = findViewById(R.id.registration_button);
        user_name = findViewById(R.id.registration_name);
        user_email = findViewById(R.id.registration_email);
        user_password = findViewById(R.id.registration_password);
        login = findViewById(R.id.Login_page);

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(user_Registration.this, login.class);
                startActivity(intent);
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registration_user();
            }
        });

    }

    public void registration_user() {
        String name = user_name.getText().toString().trim();
        String email = user_email.getText().toString().trim();
        String password = user_password.getText().toString().trim();
        double latitude = 0.0;
        double longitude = 0.0;

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all the fields", Toast.LENGTH_SHORT).show();
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String userId = user.getUid(); // Unique User ID

                            // User details Firebase Database me store karna
                            HashMap<String, Object> userData = new HashMap<>();
                            userData.put("name", name);
                            userData.put("email", email);
                            userData.put("latitude", latitude);
                            userData.put("longitude", longitude);
                            userData.put("last_updated", System.currentTimeMillis());
                            Toast.makeText(user_Registration.this, "User registered successfully!", Toast.LENGTH_SHORT).show();

                            databaseReference.child(userId).setValue(userData)
                                    .addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            Log.d("Firebase", "User data stored successfully!");
                                        }
                                    });
                        }
                    } else {
                        Log.e("Firebase", "User creation failed", task.getException());
                        Toast.makeText(user_Registration.this, "User creation failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}