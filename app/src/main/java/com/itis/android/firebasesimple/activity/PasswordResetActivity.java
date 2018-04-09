package com.itis.android.firebasesimple.activity;

import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.itis.android.firebasesimple.R;
import com.itis.android.firebasesimple.utils.SoftKeyboard;

public class PasswordResetActivity extends AppCompatActivity {

    private TextInputLayout tiEmail, tiPhone;
    private EditText etEmail, etPhone;
    private Button btnSendEmail, btnSendSMS;
    private ProgressBar progressBar;
    private View container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_reset);

        initFields();
        initClickListeners();
        initTextListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        progressBar.setVisibility(View.GONE);
    }

    private void initFields(){
        tiEmail = findViewById(R.id.ti_email);
        tiPhone = findViewById(R.id.ti_phone);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        btnSendEmail = findViewById(R.id.btn_send_email);
        btnSendSMS = findViewById(R.id.btn_send_SMS);
        progressBar = findViewById(R.id.progressBar);
        container = findViewById(R.id.container);
    }

    private void initClickListeners(){
        btnSendEmail.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                tiEmail.setError(getString(R.string.error_email));
                return;
            }
            progressBar.setVisibility(View.VISIBLE);
            SoftKeyboard.hide(container);

            FirebaseAuth auth = FirebaseAuth.getInstance();

            auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Snackbar.make(container, "E-mail send.", Snackbar.LENGTH_SHORT).show();
                        }
                        else {
                            Snackbar.make(container, "Some problems..." + task.getException(), Snackbar
                                    .LENGTH_SHORT);
                        }
                    });
            progressBar.setVisibility(View.GONE);
        });

        btnSendSMS.setOnClickListener(v -> {});
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

        etPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                tiPhone.setError(null);
            }
        });
    }
}
