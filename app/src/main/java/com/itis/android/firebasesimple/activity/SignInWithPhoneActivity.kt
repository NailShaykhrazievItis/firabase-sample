package com.itis.android.firebasesimple.activity

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
import com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks
import com.itis.android.firebasesimple.R
import com.itis.android.firebasesimple.utils.hide
import kotlinx.android.synthetic.main.activity_sign_in_with_phone.*
import java.util.concurrent.TimeUnit

class SignInWithPhoneActivity : AppCompatActivity() {

    private var verId: String? = null
    private var mToken: ForceResendingToken? = null

    private var auth: FirebaseAuth? = null
    private var verificationInProgress = false
    private var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks = initCallbacks()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in_with_phone)

        auth = FirebaseAuth.getInstance()

        initTextListeners()
        initClickListeners()
        modePhoneNumber()
    }

    override fun onStart() {
        super.onStart()
        if (verificationInProgress) {
            startVerification(et_phone.text.toString())
        }
    }

    private fun initTextListeners() {
        et_phone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable) {
                ti_phone.error = null
            }
        })

        et_code.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable) {
                ti_code.error = null
            }
        })
    }

    private fun initClickListeners() {
        btn_send_code.setOnClickListener {
            val phone = et_phone.text.toString().trim { it <= ' ' }
            if (TextUtils.isEmpty(phone)) {
                ti_phone.error = "Enter phone number!"
                return@setOnClickListener
            }
            progressBar.visibility = View.VISIBLE
            hide(container)
            startVerification(phone)
            modeVerificationCode()
        }

        btn_signin.setOnClickListener {
            signInWithPhoneAuthCredential(PhoneAuthProvider.getCredential(verId.toString(), et_code.text.toString()))
        }
    }

    private fun initCallbacks() =
            object : OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    Log.i(TAG, "onVerificationCompleted:$credential")
                    verificationInProgress = false
                    signInWithPhoneAuthCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Log.wtf(TAG, "onVerificationFailed", e)
                    verificationInProgress = false
                }

                override fun onCodeSent(verificationId: String?, token: ForceResendingToken?) {
                    Log.i(TAG, "onCodeSent:$verificationId")
                    verId = verificationId
                    mToken = token
                    modeVerificationCode()
                }
            }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_VERIFY_IN_PROGRESS, verificationInProgress)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        verificationInProgress = savedInstanceState.getBoolean(KEY_VERIFY_IN_PROGRESS)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth?.signInWithCredential(credential)?.addOnCompleteListener(this) {
            if (it.isSuccessful) {
                // Sign in success, update UI with the signed-in user's information
                Log.d(TAG, "signInWithCredential:success")
                val intent = Intent(this@SignInWithPhoneActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                // ...
            } else {
                // Sign in failed, display a message and update the UI
                Log.w(TAG, "signInWithCredential:failure", it.exception)
                if (it.exception is FirebaseAuthInvalidCredentialsException) {
                    // The verification code entered was invalid
                    ti_code.error = "Invalid verification code"
                }
            }
        }
    }

    private fun modePhoneNumber() {
        ti_phone.visibility = View.VISIBLE
        btn_send_code.visibility = View.VISIBLE
        ti_code.visibility = View.GONE
        btn_signin.visibility = View.GONE
    }

    private fun modeVerificationCode() {
        ti_phone.visibility = View.GONE
        btn_send_code.visibility = View.GONE
        ti_code.visibility = View.VISIBLE
        btn_signin.visibility = View.VISIBLE
    }

    private fun startVerification(phone: String) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(phone, 120, TimeUnit.SECONDS, this, callbacks)
        verificationInProgress = true
    }

    companion object {

        private val TAG = "SignInWithPhoneActivity"
        private val KEY_VERIFY_IN_PROGRESS = "ver in progress"
    }
}
