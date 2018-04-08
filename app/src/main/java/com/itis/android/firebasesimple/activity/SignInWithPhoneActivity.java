package com.itis.android.firebasesimple.activity;

import android.content.Intent;
import android.os.Bundle;
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
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken;
import com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks;
import com.itis.android.firebasesimple.R;
import com.itis.android.firebasesimple.utils.SoftKeyboard;
import java.util.concurrent.TimeUnit;

public class SignInWithPhoneActivity extends AppCompatActivity {

    private static final String TAG = "SignInWithPhoneActivity";

    private static final String KEY_VERIFY_IN_PROGRESS = "ver in progress";

    private TextInputLayout tiCode, tiPhone;
    private EditText etPhone, etCode;
    private Button btnSendCode, btnSignIn;
    private ProgressBar progressBar;
    private View container;
    private String verId;
    private ForceResendingToken mToken;

    private FirebaseAuth auth;
    private boolean verificationInProgress = false;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in_with_phone);

        auth = FirebaseAuth.getInstance();

        initViews();
        initTextListeners();
        initClickListeners();
        initCallbacks();
        modePhoneNumber();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (verificationInProgress) {
            startVerification(etPhone.getText().toString());
        }
    }

    private void initViews() {
        tiCode = findViewById(R.id.ti_code);
        etCode = findViewById(R.id.et_code);
        tiPhone = findViewById(R.id.ti_phone);
        etPhone = findViewById(R.id.et_phone);
        btnSendCode = findViewById(R.id.btn_send_code);
        btnSignIn = findViewById(R.id.btn_signin);
        progressBar = findViewById(R.id.progressBar);
        container = findViewById(R.id.container);
    }

    private void initTextListeners() {
        etPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {

            }

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {

            }

            @Override
            public void afterTextChanged(final Editable s) {
                tiPhone.setError(null);
            }
        });

        etCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {

            }

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {

            }

            @Override
            public void afterTextChanged(final Editable s) {
                tiCode.setError(null);
            }
        });
    }

    private void initClickListeners() {
        btnSendCode.setOnClickListener(l -> {
            String phone = etPhone.getText().toString().trim();
            if (TextUtils.isEmpty(phone)) {
                tiPhone.setError("Enter phone number!");
                return;
            }
            progressBar.setVisibility(View.VISIBLE);
            SoftKeyboard.hide(container);
            startVerification(phone);
            modeVerificationCode();
        });

        btnSignIn.setOnClickListener(l ->
                signInWithPhoneAuthCredential(PhoneAuthProvider.getCredential(verId, etCode.getText().toString())));
    }

    private void initCallbacks() {
        callbacks = new OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                Log.i(TAG, "onVerificationCompleted:" + credential);
                verificationInProgress = false;
                signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                Log.wtf(TAG, "onVerificationFailed", e);
                verificationInProgress = false;
            }

            @Override
            public void onCodeSent(final String verificationId, final ForceResendingToken token) {
                Log.i(TAG, "onCodeSent:" + verificationId);
                verId = verificationId;
                mToken = token;
                modeVerificationCode();
            }
        };
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_VERIFY_IN_PROGRESS, verificationInProgress);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        verificationInProgress = savedInstanceState.getBoolean(KEY_VERIFY_IN_PROGRESS);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithCredential:success");
                        Intent intent = new Intent(SignInWithPhoneActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        // ...
                    } else {
                        // Sign in failed, display a message and update the UI
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            // The verification code entered was invalid
                            tiCode.setError("Invalid verification code");
                        }
                    }
                });
    }

    private void modePhoneNumber() {
        tiPhone.setVisibility(View.VISIBLE);
        btnSendCode.setVisibility(View.VISIBLE);
        tiCode.setVisibility(View.GONE);
        btnSignIn.setVisibility(View.GONE);
    }

    private void modeVerificationCode() {
        tiPhone.setVisibility(View.GONE);
        btnSendCode.setVisibility(View.GONE);
        tiCode.setVisibility(View.VISIBLE);
        btnSignIn.setVisibility(View.VISIBLE);
    }

    private void startVerification(String phone) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(phone, 120, TimeUnit.SECONDS, this, callbacks);
        verificationInProgress = true;
    }
}
