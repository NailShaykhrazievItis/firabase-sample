package com.itis.android.firebasesimple.activity

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

import com.afollestad.materialdialogs.MaterialDialog
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.itis.android.firebasesimple.R

import java.util.concurrent.TimeUnit

class SignInViaPhoneNumberActivity : AppCompatActivity() {

    private var etPhoneNumber: EditText? = null
    private var btnSignUp: Button? = null
    private var btnBack: Button? = null

    private var firebaseAuth: FirebaseAuth? = null

    private var verificationId: String? = null
    private var token: PhoneAuthProvider.ForceResendingToken? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in_via_phone_number)

        firebaseAuth = FirebaseAuth.getInstance()

        initFields()
        initClickListeners()

    }

    private fun initClickListeners() {
        btnBack!!.setOnClickListener { v -> finish() }

        btnSignUp!!.setOnClickListener { v ->
            val phoneNumber = etPhoneNumber!!.text.toString()
            Log.d("Alm", "number: $phoneNumber")
            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                    phoneNumber,
                    60,
                    TimeUnit.SECONDS,
                    this,
                    object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {
                            Log.d("Alm", "onVerificationCompleted")
                        }

                        override fun onVerificationFailed(e: FirebaseException) {
                            Log.e("Alm", "onVerificationFailed")
                            Log.e("Alm", e.toString())
                        }

                        override fun onCodeSent(s: String?, forceResendingToken: PhoneAuthProvider.ForceResendingToken?) {
                            super.onCodeSent(s, forceResendingToken)
                            Log.d("Alm", "onCodeSent to $phoneNumber")
                            verificationId = s
                            token = forceResendingToken
                            MaterialDialog.Builder(this@SignInViaPhoneNumberActivity)
                                    .title(R.string.write_code)
                                    .inputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
                                    .input(R.string.verification_code, R.string.empty) { dialog, input ->
                                        Log.d("Alm", "onInput")
                                        val credential = PhoneAuthProvider
                                                .getCredential(verificationId!!, input.toString())
                                        signInWithPhoneAuthCredential(credential)
                                    }.show()
                        }
                    })
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        firebaseAuth!!.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    Log.d("Alm", "signInWithCredential:onComplete:" + task.isSuccessful)

                    if (!task.isSuccessful) {
                        Log.w("Alm", "signInWithCredential ", task.exception)
                        Toast.makeText(this@SignInViaPhoneNumberActivity, "Authentication failed.",
                                Toast.LENGTH_SHORT).show()
                    } else {
                        //                        FirebaseUser user = task.getResult().getUser();
                        startActivity(Intent(this@SignInViaPhoneNumberActivity, MainActivity::class.java))
                        finish()
                    }
                }
    }

    private fun initFields() {
        etPhoneNumber = findViewById(R.id.et_phone_number)
        btnSignUp = findViewById(R.id.btn_sign_up)
        btnBack = findViewById(R.id.btn_back)
    }
}
