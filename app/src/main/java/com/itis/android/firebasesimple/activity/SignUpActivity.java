package com.itis.android.firebasesimple.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.itis.android.firebasesimple.R;
import com.itis.android.firebasesimple.utils.SoftKeyboard;

/**
 * Created by Nail Shaykhraziev on 02.04.2018.
 */
public class SignUpActivity extends AppCompatActivity {

    private TextInputLayout tiEmail, tiPassword;
    private EditText etEmail, etPassword;
    private Button btnSignIn, btnSignUp;
    private ProgressBar progressBar;
    private View container;

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        //Get Firebase auth instance
        auth = FirebaseAuth.getInstance();

        initFields();

        initClickListeners();

        initTextListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        progressBar.setVisibility(View.GONE);
    }

    private void initFields() {
        container = findViewById(R.id.container);
        btnSignIn = findViewById(R.id.btn_to_signin);
        btnSignUp = findViewById(R.id.btn_signup);
        tiEmail = findViewById(R.id.ti_email);
        tiPassword = findViewById(R.id.ti_password);
        etEmail = findViewById(R.id.email);
        etPassword = findViewById(R.id.password);
        progressBar = findViewById(R.id.progressBar);
    }

    private void initTextListeners() {
        etEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                tiEmail.setError(null);
            }
        });
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                tiPassword.setError(null);
            }
        });
    }

    private void initClickListeners() {
        btnSignIn.setOnClickListener(v -> finish());

        btnSignUp.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                tiEmail.setError(getString(R.string.error_email));
                return;
            }
            if (TextUtils.isEmpty(password)) {
                tiPassword.setError(getString(R.string.error_pass));
                return;
            }
            if (password.length() < 6) {
                tiPassword.setError(getString(R.string.error_pass_length));
                return;
            }
            progressBar.setVisibility(View.VISIBLE);
            SoftKeyboard.hide(container);
            //create user
            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(SignUpActivity.this, task -> {
                        Toast.makeText(SignUpActivity.this,
                                "createUserWithEmail:onComplete:" + task.isSuccessful(), Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Snackbar.make(container, "Authentication failed." +
                                    task.getException(), Snackbar.LENGTH_SHORT).show();
                        } else {
                            startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                            finish();
                        }
                    });
        });
    }
}