package com.itis.android.firebasesimple.activity;

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
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks;
import com.itis.android.firebasesimple.R;
import com.itis.android.firebasesimple.utils.SoftKeyboard;
import java.util.concurrent.TimeUnit;

public class SignInWithPhoneActivity extends AppCompatActivity {

    private static final String TAG = "SignInWithPhoneActivity";

    private TextInputLayout tiCode, tiPhone;
    private EditText etPhone;
    private Button btnSendCode;
    private ProgressBar progressBar;
    private View container;

    private FirebaseAuth auth;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in_with_phone);

        auth = FirebaseAuth.getInstance();

        initViews();

        initTextListeners();
    }

    private void initViews() {
        tiCode = findViewById(R.id.ti_code);
        tiPhone = findViewById(R.id.ti_phone);
        etPhone = findViewById(R.id.et_phone);
        btnSendCode = findViewById(R.id.btn_send_code);
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
            PhoneAuthProvider.getInstance().verifyPhoneNumber(phone, 10, TimeUnit.MINUTES, this, callbacks);
        });
    }

    private void initCallbacks() {
        callbacks = new OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                Log.i(TAG, "onVerificationCompleted:" + credential);

            }

            @Override
            public void onVerificationFailed(FirebaseException e) {

            }
        };
    }
}
