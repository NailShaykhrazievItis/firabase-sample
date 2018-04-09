package com.itis.android.firebasesimple.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.itis.android.firebasesimple.R;

import java.util.concurrent.TimeUnit;

public class SignInViaPhoneNumberActivity extends AppCompatActivity {

    private EditText etPhoneNumber;
    private Button btnSignUp, btnBack;

    private FirebaseAuth firebaseAuth;

    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in_via_phone_number);

        firebaseAuth = FirebaseAuth.getInstance();

        initFields();
        initClickListeners();

    }

    private void initClickListeners() {
        btnBack.setOnClickListener(v -> {
            finish();
        });

        btnSignUp.setOnClickListener(v -> {
            String phoneNumber = etPhoneNumber.getText().toString();
            Log.d("Alm", "number: " + phoneNumber);
            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                    phoneNumber,
                    60,
                    TimeUnit.SECONDS,
                    this,
                    new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        @Override
                        public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                            Log.d("Alm", "onVerificationCompleted");
                        }

                        @Override
                        public void onVerificationFailed(FirebaseException e) {
                            Log.e("Alm", "onVerificationFailed");
                            Log.e("Alm", e.toString());
                        }

                        @Override
                        public void onCodeSent(String s, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                            super.onCodeSent(s, forceResendingToken);
                            Log.d("Alm", "onCodeSent to " + phoneNumber);
                            verificationId = s;
                            token = forceResendingToken;
                            new MaterialDialog.Builder(SignInViaPhoneNumberActivity.this)
                                    .title(R.string.write_code)
                                    .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
                                    .input(R.string.verification_code, R.string.empty, new MaterialDialog.InputCallback() {
                                        @Override
                                        public void onInput(MaterialDialog dialog, CharSequence input) {
                                            Log.d("Alm", "onInput");
                                            PhoneAuthCredential credential = PhoneAuthProvider
                                                    .getCredential(verificationId, input.toString());
                                            signInWithPhoneAuthCredential(credential);
                                        }
                                    }).show();
                        }
                    });
        });
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    Log.d("Alm", "signInWithCredential:onComplete:" +
                            task.isSuccessful());

                    if (!task.isSuccessful()) {
                        Log.w("Alm", "signInWithCredential ", task.getException());
                        Toast.makeText(SignInViaPhoneNumberActivity.this, "Authentication failed.",
                                Toast.LENGTH_SHORT).show();
                    } else {
//                        FirebaseUser user = task.getResult().getUser();
                        startActivity(new Intent(SignInViaPhoneNumberActivity.this, MainActivity.class));
                        finish();
                    }
                });
    }

    private void initFields() {
        etPhoneNumber = findViewById(R.id.et_phone_number);
        btnSignUp = findViewById(R.id.btn_sign_up);
        btnBack = findViewById(R.id.btn_back);
    }
}
