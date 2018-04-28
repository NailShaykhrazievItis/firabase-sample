package com.itis.android.firebasesimple.activity

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.util.Log
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.itis.android.firebasesimple.R
import kotlinx.android.synthetic.main.activity_sign_in_via_phone_number.*
import java.util.concurrent.TimeUnit

class SignInViaPhoneNumberActivity : AppCompatActivity() {

    private var firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    private var token: PhoneAuthProvider.ForceResendingToken? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in_via_phone_number)

        initClickListeners()
    }

    private fun initClickListeners() {
        btn_back.setOnClickListener { finish() }

        btn_sign_up.setOnClickListener {
            val phoneNumber = et_phone_number.text.toString()
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

                        override fun onCodeSent(s: String, forceResendingToken: PhoneAuthProvider.ForceResendingToken?) {
                            super.onCodeSent(s, forceResendingToken)
                            Log.d("Alm", "onCodeSent to $phoneNumber")
                            token = forceResendingToken
                            MaterialDialog.Builder(this@SignInViaPhoneNumberActivity)
                                    .title(R.string.write_code)
                                    .inputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
                                    .input(R.string.verification_code, R.string.empty) { dialog, input ->
                                        Log.d("Alm", "onInput")
                                        val credential = PhoneAuthProvider
                                                .getCredential(s, input.toString())
                                        signInWithPhoneAuthCredential(credential)
                                    }.show()
                        }
                    })
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this) {
                    Log.d("Alm", "signInWithCredential:onComplete:" + it.isSuccessful)

                    if (!it.isSuccessful) {
                        Log.w("Alm", "signInWithCredential ", it.exception)
                        Toast.makeText(this@SignInViaPhoneNumberActivity, "Authentication failed.",
                                Toast.LENGTH_SHORT).show()
                    } else {
                        //                        FirebaseUser user = task.getResult().getUser();
                        startActivity(Intent(this@SignInViaPhoneNumberActivity, MainActivity::class.java))
                        finish()
                    }
                }
    }
}
