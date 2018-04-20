package com.itis.android.firebasesimple.activity

import android.content.Intent
import android.os.Bundle
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
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
import com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks
import com.itis.android.firebasesimple.R
import com.itis.android.firebasesimple.utils.SoftKeyboard
import java.util.concurrent.TimeUnit

class SignInWithPhoneActivity : AppCompatActivity() {

    private var tiCode: TextInputLayout? = null
    private var tiPhone: TextInputLayout? = null
    private var etPhone: EditText? = null
    private var etCode: EditText? = null
    private var btnSendCode: Button? = null
    private var btnSignIn: Button? = null
    private var progressBar: ProgressBar? = null
    private var container: View? = null
    private var verId: String? = null
    private var mToken: ForceResendingToken? = null

    private var auth: FirebaseAuth? = null
    private var verificationInProgress = false
    private var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in_with_phone)

        auth = FirebaseAuth.getInstance()

        initViews()
        initTextListeners()
        initClickListeners()
        initCallbacks()
        modePhoneNumber()
    }

    override fun onStart() {
        super.onStart()
        if (verificationInProgress) {
            startVerification(etPhone!!.text.toString())
        }
    }

    private fun initViews() {
        tiCode = findViewById(R.id.ti_code)
        etCode = findViewById(R.id.et_code)
        tiPhone = findViewById(R.id.ti_phone)
        etPhone = findViewById(R.id.et_phone)
        btnSendCode = findViewById(R.id.btn_send_code)
        btnSignIn = findViewById(R.id.btn_signin)
        progressBar = findViewById(R.id.progressBar)
        container = findViewById(R.id.container)
    }

    private fun initTextListeners() {
        etPhone!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable) {
                tiPhone!!.error = null
            }
        })

        etCode!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable) {
                tiCode!!.error = null
            }
        })
    }

    private fun initClickListeners() {
        btnSendCode!!.setOnClickListener { l ->
            val phone = etPhone!!.text.toString().trim { it <= ' ' }
            if (TextUtils.isEmpty(phone)) {
                tiPhone!!.error = "Enter phone number!"
                return@setOnClickListener
            }
            progressBar!!.visibility = View.VISIBLE
            SoftKeyboard.hide(container!!)
            startVerification(phone)
            modeVerificationCode()
        }

        btnSignIn!!.setOnClickListener { l ->
            signInWithPhoneAuthCredential(PhoneAuthProvider.getCredential(verId!!, etCode!!.text.toString()))
        }
    }

    private fun initCallbacks() {
        callbacks = object : OnVerificationStateChangedCallbacks() {
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
                Log.i(TAG, "onCodeSent:" + verificationId!!)
                verId = verificationId
                mToken = token
                modeVerificationCode()
            }
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
        auth!!.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithCredential:success")
                        val intent = Intent(this@SignInWithPhoneActivity, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        // ...
                    } else {
                        // Sign in failed, display a message and update the UI
                        Log.w(TAG, "signInWithCredential:failure", task.exception)
                        if (task.exception is FirebaseAuthInvalidCredentialsException) {
                            // The verification code entered was invalid
                            tiCode!!.error = "Invalid verification code"
                        }
                    }
                }
    }

    private fun modePhoneNumber() {
        tiPhone!!.visibility = View.VISIBLE
        btnSendCode!!.visibility = View.VISIBLE
        tiCode!!.visibility = View.GONE
        btnSignIn!!.visibility = View.GONE
    }

    private fun modeVerificationCode() {
        tiPhone!!.visibility = View.GONE
        btnSendCode!!.visibility = View.GONE
        tiCode!!.visibility = View.VISIBLE
        btnSignIn!!.visibility = View.VISIBLE
    }

    private fun startVerification(phone: String) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(phone, 120, TimeUnit.SECONDS, this, callbacks!!)
        verificationInProgress = true
    }

    companion object {

        private val TAG = "SignInWithPhoneActivity"

        private val KEY_VERIFY_IN_PROGRESS = "ver in progress"
    }
}
