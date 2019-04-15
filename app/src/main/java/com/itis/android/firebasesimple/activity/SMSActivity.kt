package com.itis.android.firebasesimple.activity

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.text.TextUtils
import android.view.View
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.itis.android.firebasesimple.R
import kotlinx.android.synthetic.main.activity_sms.*
import java.util.concurrent.TimeUnit

class SMSActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private var storedVerificationId: String? = ""
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sms)
        auth = FirebaseAuth.getInstance()
        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                updateUI(STATE_VERIFY_SUCCESS)
                signInWithPhoneAuthCredential(credential)
                val intent = Intent(this@SMSActivity, MainActivity::class.java)
                startActivity(intent)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                if (e is FirebaseAuthInvalidCredentialsException) {
                    et_phone_number.error = "Invalid phone number."
                } else if (e is FirebaseTooManyRequestsException) {
                    Snackbar.make(findViewById(android.R.id.content), "Quota exceeded.",
                            Snackbar.LENGTH_SHORT).show()
                }
                updateUI(STATE_VERIFY_FAILED)
            }

            override fun onCodeSent(
                    verificationId: String?,
                    token: PhoneAuthProvider.ForceResendingToken
            ) {
                storedVerificationId = verificationId
                resendToken = token
                updateUI(STATE_CODE_SENT)
            }
        }
        setListeners()
    }

    private fun startPhoneNumberVerification(phoneNumber: String) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,
                60,
                TimeUnit.SECONDS,
                this,
                callbacks)
    }

    private fun verifyPhoneNumberWithCode(verificationId: String?, code: String) {
        verificationId?.let {
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            signInWithPhoneAuthCredential(credential)
        }
    }

    private fun resendVerificationCode(
            phoneNumber: String,
            token: PhoneAuthProvider.ForceResendingToken?
    ) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,
                60,
                TimeUnit.SECONDS,
                this,
                callbacks,
                token)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        updateUI(STATE_SIGNIN_SUCCESS)
                    } else {
                        if (task.exception is FirebaseAuthInvalidCredentialsException) {
                            et_verification_code.error = "Invalid code."
                        }
                        updateUI(STATE_SIGNIN_FAILED)
                    }
                }
    }

    private fun updateUI(
            uiState: Int
    ) {
        when (uiState) {
            STATE_CODE_SENT -> {
                enableViews(btn_verify, btn_resend, et_verification_code, et_phone_number)
                disableViews(btn_send_code)
                tv_status.setText(R.string.status_code_sent)
            }
            STATE_VERIFY_FAILED -> {
                enableViews(btn_verify, btn_send_code, btn_resend, et_phone_number,
                        et_verification_code)
                tv_status.setText(R.string.status_verification_failed)
            }
        }
    }

    private fun validatePhoneNumber(): Boolean {
        val phoneNumber = et_phone_number.text.toString()
        if (TextUtils.isEmpty(phoneNumber)) {
            et_phone_number.error = "Invalid phone number."
            return false
        }
        return true
    }

    private fun enableViews(vararg views: View) {
        for (v in views) {
            v.isEnabled = true
        }
    }

    private fun disableViews(vararg views: View) {
        for (v in views) {
            v.isEnabled = false
        }
    }

    private fun setListeners() {
        btn_send_code.setOnClickListener {
            if (!validatePhoneNumber()) {
                return@setOnClickListener
            }
            startPhoneNumberVerification(et_phone_number.text.toString())
        }

        btn_verify.setOnClickListener {
            val code = et_verification_code.text.toString()
            if (TextUtils.isEmpty(code)) {
                et_verification_code.error = "Cannot be empty."
                return@setOnClickListener
            }
            verifyPhoneNumberWithCode(storedVerificationId, code)
        }

        btn_resend.setOnClickListener {
            resendVerificationCode(et_phone_number.text.toString(), resendToken)
        }
    }

    companion object {
        private const val STATE_VERIFY_FAILED = 3
        private const val STATE_VERIFY_SUCCESS = 4
        private const val STATE_CODE_SENT = 2
        private const val STATE_SIGNIN_FAILED = 5
        private const val STATE_SIGNIN_SUCCESS = 6
    }

}
