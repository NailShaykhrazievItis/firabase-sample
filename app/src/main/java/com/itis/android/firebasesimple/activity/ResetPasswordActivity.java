package com.itis.android.firebasesimple.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.itis.android.firebasesimple.R;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText etEmail;

    private Button btnResetPassword, btnBack;

    private FirebaseAuth firebaseAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        firebaseAuth = FirebaseAuth.getInstance();

        initFields();
        initClickListeners();
    }

    private void initClickListeners() {
        btnBack.setOnClickListener(v -> {
            finish();
        });

        btnResetPassword.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(ResetPasswordActivity.this, getString(R.string.error_email), Toast.LENGTH_SHORT)
                        .show();
            } else {
                firebaseAuth.sendPasswordResetEmail(email)
                        .addOnCompleteListener(command -> {
                            Toast.makeText(ResetPasswordActivity.this, getString(R.string.check_email),
                                    Toast.LENGTH_SHORT).show();
                        });
            }
        });
    }

    private void initFields() {
        etEmail = findViewById(R.id.email);
        btnResetPassword = findViewById(R.id.btn_reset);
        btnBack = findViewById(R.id.btn_back);
    }
}
