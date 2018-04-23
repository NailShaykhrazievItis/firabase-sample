package com.itis.android.firebasesimple.activity

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.design.widget.TextInputLayout
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar

import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.itis.android.firebasesimple.R
import com.itis.android.firebasesimple.utils.SoftKeyboard
import kotlinx.android.synthetic.main.activity_sign_in_phone_number.*

import java.util.concurrent.TimeUnit

/**
 * Created by Nail Shaykhraziev on 02.04.2018.
 */
class SignInPhoneNumberActivity : AppCompatActivity() {

    private var phoneAuthCredential: PhoneAuthCredential? = null
    private var auth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in_phone_number)

        //Get Firebase auth instance
        auth = FirebaseAuth.getInstance()

        initFields()

        initClickListeners()

        initTextListeners()
    }

    override fun onResume() {
        super.onResume()
        progressBar?.visibility = View.GONE
    }

    private fun initFields() {
        code?.visibility = View.GONE
        btn_login?.visibility = View.GONE
        btn_login?.isEnabled = false
    }

    private fun initTextListeners() {
        phone_number?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.toString().trim { it <= ' ' }.length > 0) {
                    btn_send_code?.isEnabled = true
                } else {
                    btn_send_code?.isEnabled = false
                }
            }

            override fun afterTextChanged(s: Editable) {
                ti_phone_number?.error = null
            }
        })
        code?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.toString().trim { it <= ' ' }.length > 0) {
                    btn_login?.isEnabled = true
                } else {
                    btn_login?.isEnabled = false
                }
            }

            override fun afterTextChanged(s: Editable) {
                ti_code?.error = null
            }
        })
    }

    private fun initClickListeners() {
        btn_to_signin?.setOnClickListener { v -> finish() }

        btn_to_signin?.setOnClickListener { v ->
            progressBar?.visibility = View.VISIBLE
            SoftKeyboard.hide(container)
            startActivity(Intent(this@SignInPhoneNumberActivity, SignUpActivity::class.java))
        }

        btn_login?.setOnClickListener { v ->
            progressBar?.visibility = View.VISIBLE
            SoftKeyboard.hide(container)
            if (phoneAuthCredential != null) {
                signInWithPhoneAuthCredential(phoneAuthCredential!!)
            }
        }

        btn_send_code?.setOnClickListener { v ->
            code?.visibility = View.VISIBLE
            btn_login?.visibility = View.VISIBLE

            /* ~ Testing via emulator
            java.lang.NullPointerException: Attempt to invoke virtual method
            'com.google.android.gms.tasks.Task com.google.android.gms.common.api.GoogleApi.zzb(com.google.android.gms.common.api.internal.zzde)' on a null object reference
             */
            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                    phone_number?.text.toString(),
                    PHONE_AUTH_TIMEOUT_DURATION.toLong(),
                    TimeUnit.SECONDS,
                    this,
                    object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {
                            Log.d(TAG, "onVerificationCompleted:$phoneAuthCredential")
                            signInWithPhoneAuthCredential(phoneAuthCredential)
                        }

                        override fun onVerificationFailed(e: FirebaseException) {
                            Log.d(TAG, "onVerificationFailed", e)

                            if (e is FirebaseAuthInvalidCredentialsException) {
                                Log.w(TAG, "Invalid request", e)
                            } else if (e is FirebaseTooManyRequestsException) {
                                Log.w(TAG, "The SMS quota for the project has been exceeded", e)
                            }
                            if (container != null) {
                                Snackbar.make(container,
                                R.string.verification_failed,
                                Snackbar.LENGTH_SHORT).show()
                            }
                        }

                        override fun onCodeSent(verificationId: String?, forceResendingToken: PhoneAuthProvider.ForceResendingToken?) {
                            if (verificationId != null) {
                                Log.d(TAG, "onCodeSent" + verificationId)
                                val code = code?.text.toString().trim { it <= ' ' }
                                if (TextUtils.isEmpty(code)) {
                                    ti_code?.error = getString(R.string.error_email)
                                    return
                                }
                                phoneAuthCredential = PhoneAuthProvider.getCredential(verificationId, code)
                            }
                        }
                    }
            )
        }
    }

    private fun signInWithPhoneAuthCredential(phoneAuthCredential: PhoneAuthCredential) {
        auth?.signInWithCredential(phoneAuthCredential)
                ?.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "signInWithPhoneAuthCredential succeed")
                        startActivity(Intent(this@SignInPhoneNumberActivity, MainActivity::class.java))
                        finish()
                    } else {
                        if (task.exception != null) {
                            Log.w(TAG, "signInWithPhoneAuthCredential failed " + task.exception)
                            if (task.exception is FirebaseAuthInvalidCredentialsException) {
                                Snackbar.make(container, "Authentication failed." + task.exception, Snackbar.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
    }

    companion object {

        val PHONE_AUTH_TIMEOUT_DURATION = 60
        val TAG = "SignUp"
    }
}