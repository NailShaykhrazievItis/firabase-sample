package com.itis.android.firebasesimple.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
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
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.itis.android.firebasesimple.R;
import com.itis.android.firebasesimple.utils.SoftKeyboard;

import java.util.concurrent.TimeUnit;

/**
 * Created by Nail Shaykhraziev on 02.04.2018.
 */
public class SignInPhoneNumberActivity extends AppCompatActivity {

    public static final int PHONE_AUTH_TIMEOUT_DURATION = 60;
    public static final String TAG = "SignUp";

    private TextInputLayout tiPhoneNumber, tiCode;
    private EditText etPhoneNumber, etCode;
    private Button btnSendCode, btnLogin, btnSignIn, btnSignUp;
    private ProgressBar progressBar;
    private View container;

    private PhoneAuthCredential phoneAuthCredential;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in_phone_number);

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
        btnLogin = findViewById(R.id.btn_login);
        btnSignIn = findViewById(R.id.btn_to_signin);
        btnSignUp = findViewById(R.id.btn_signup);
        btnSendCode = findViewById(R.id.btn_send_code);
        tiPhoneNumber = findViewById(R.id.ti_phone_number);
        tiCode = findViewById(R.id.ti_code);
        etPhoneNumber = findViewById(R.id.phone_number);
        etCode = findViewById(R.id.code);
        progressBar = findViewById(R.id.progressBar);

        etCode.setVisibility(View.GONE);
        btnLogin.setVisibility(View.GONE);
        btnLogin.setEnabled(false);
    }

    private void initTextListeners() {
        etPhoneNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() > 0) {
                    btnSendCode.setEnabled(true);
                } else {
                    btnSendCode.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                tiPhoneNumber.setError(null);
            }
        });
        etCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() > 0) {
                    btnLogin.setEnabled(true);
                } else {
                    btnLogin.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                tiCode.setError(null);
            }
        });
    }

    private void initClickListeners() {
        btnSignIn.setOnClickListener(v -> finish());

        btnSignUp.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            SoftKeyboard.hide(container);
            startActivity(new Intent(SignInPhoneNumberActivity.this, SignUpActivity.class));
        });

        btnLogin.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            SoftKeyboard.hide(container);
            if (phoneAuthCredential != null) {
                signInWithPhoneAuthCredential(phoneAuthCredential);
            }
        });

        btnSendCode.setOnClickListener(v -> {
            etCode.setVisibility(View.VISIBLE);
            btnLogin.setVisibility(View.VISIBLE);

            /* ~ Testing via emulator
            java.lang.NullPointerException: Attempt to invoke virtual method
            'com.google.android.gms.tasks.Task com.google.android.gms.common.api.GoogleApi.zzb(com.google.android.gms.common.api.internal.zzde)' on a null object reference
             */
            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                    etPhoneNumber.getText().toString(),
                    PHONE_AUTH_TIMEOUT_DURATION,
                    TimeUnit.SECONDS,
                    this,
                    new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        @Override
                        public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                            Log.d(TAG, "onVerificationCompleted:" + phoneAuthCredential);
                            signInWithPhoneAuthCredential(phoneAuthCredential);
                        }

                        @Override
                        public void onVerificationFailed(FirebaseException e) {
                            Log.d(TAG, "onVerificationFailed", e);

                            if (e instanceof FirebaseAuthInvalidCredentialsException) {
                                Log.w(TAG, "Invalid request", e);
                            } else if (e instanceof FirebaseTooManyRequestsException) {
                                Log.w(TAG, "The SMS quota for the project has been exceeded", e);
                            }
                            Snackbar.make(container,
                                    R.string.verification_failed,
                                    Snackbar.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                            Log.d(TAG, "onCodeSent" + verificationId);
                            String code = etCode.getText().toString().trim();
                            if (TextUtils.isEmpty(code)) {
                                tiCode.setError(getString(R.string.error_email));
                                return;
                            }
                            phoneAuthCredential = PhoneAuthProvider.getCredential(verificationId, code);
                        }
                    }
            );
        });
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential phoneAuthCredential) {
        auth.signInWithCredential(phoneAuthCredential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithPhoneAuthCredential succeed");
                            startActivity(new Intent(SignInPhoneNumberActivity.this, MainActivity.class));
                            finish();
                        } else {
                            Log.w(TAG, "signInWithPhoneAuthCredential failed " + task.getException());
                            if (task.getException() instanceof
                                    FirebaseAuthInvalidCredentialsException) {
                                Snackbar.make(container, "Authentication failed." +
                                        task.getException(), Snackbar.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }
}