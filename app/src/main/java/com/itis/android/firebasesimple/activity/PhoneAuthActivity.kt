package com.itis.android.firebasesimple.activity

import android.content.Intent
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.itis.android.firebasesimple.R
import com.itis.android.firebasesimple.utils.SoftKeyboard

import java.util.concurrent.TimeUnit

class PhoneAuthActivity : AppCompatActivity(), View.OnClickListener {

    private var mAuth: FirebaseAuth? = null

    private var mVerificationInProgress = false
    private var mVerificationId: String? = null
    private var mResendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var mCallbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks? = null

    private var mPhoneNumberViews: ViewGroup? = null

    private var mStatusText: TextView? = null
    private var mDetailText: TextView? = null

    private var mPhoneNumberField: EditText? = null
    private var mVerificationField: EditText? = null

    private var mStartButton: Button? = null
    private var mVerifyButton: Button? = null
    private var mResendButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone_auth)

        // Restore instance state
        if (savedInstanceState != null) {
            onRestoreInstanceState(savedInstanceState)
        }

        mAuth = FirebaseAuth.getInstance()

        initFields()
        initClickListeners()

        // Initialize phone auth callbacks
        mCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verification without
                //     user action.
                Log.d(TAG, "onVerificationCompleted:$credential")

                mVerificationInProgress = false


                // Update the UI and attempt sign in with the phone credential
                updateUI(STATE_VERIFY_SUCCESS, credential)

                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                Log.w(TAG, "onVerificationFailed", e)

                mVerificationInProgress = false

                if (e is FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                    mPhoneNumberField!!.error = "Invalid phone number."
                } else if (e is FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                    Snackbar.make(findViewById(android.R.id.content), "Quota exceeded.",
                            Snackbar.LENGTH_SHORT).show()
                }

                // Show a message and update the UI
                updateUI(STATE_VERIFY_FAILED)
            }

            override fun onCodeSent(verificationId: String?,
                                    token: PhoneAuthProvider.ForceResendingToken?) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.
                Log.d(TAG, "onCodeSent:" + verificationId!!)

                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId
                mResendToken = token

                // Update UI
                updateUI(STATE_CODE_SENT)
            }
        }
    }

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = mAuth!!.currentUser
        updateUI(currentUser)

        if (mVerificationInProgress && validatePhoneNumber()) {
            startPhoneNumberVerification(mPhoneNumberField!!.text.toString())
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_VERIFY_IN_PROGRESS, mVerificationInProgress)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        mVerificationInProgress = savedInstanceState.getBoolean(KEY_VERIFY_IN_PROGRESS)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.button_start_verification -> {
                if (!validatePhoneNumber()) {
                    return
                }
                startPhoneNumberVerification(mPhoneNumberField!!.text.toString())
            }
            R.id.button_verify_phone -> {
                val code = mVerificationField!!.text.toString()
                if (TextUtils.isEmpty(code)) {
                    mVerificationField!!.error = "Cannot be empty."
                    return
                }
                verifyPhoneNumberWithCode(mVerificationId, code)
            }
            R.id.button_resend -> resendVerificationCode(mPhoneNumberField!!.text.toString(), mResendToken)
        }
    }

    private fun initClickListeners() {
        mStartButton!!.setOnClickListener(this)
        mVerifyButton!!.setOnClickListener(this)
        mResendButton!!.setOnClickListener(this)
    }

    private fun initFields() {
        mPhoneNumberViews = findViewById(R.id.phone_auth_fields)

        mStatusText = findViewById(R.id.status)
        mDetailText = findViewById(R.id.detail)

        mPhoneNumberField = findViewById(R.id.field_phone_number)
        mVerificationField = findViewById(R.id.field_verification_code)

        mStartButton = findViewById(R.id.button_start_verification)
        mVerifyButton = findViewById(R.id.button_verify_phone)
        mResendButton = findViewById(R.id.button_resend)
    }

    private fun startPhoneNumberVerification(phoneNumber: String) {
        // [START start_phone_auth]
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber, // Phone number to verify
                60, // Timeout duration
                TimeUnit.SECONDS, // Unit of timeout
                this, // Activity (for callback binding)
                mCallbacks!!)        // OnVerificationStateChangedCallbacks
        // [END start_phone_auth]

        mVerificationInProgress = true
    }

    private fun verifyPhoneNumberWithCode(verificationId: String?, code: String) {
        // [START verify_with_code]
        val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
        // [END verify_with_code]
        signInWithPhoneAuthCredential(credential)
    }

    private fun resendVerificationCode(phoneNumber: String,
                                       token: PhoneAuthProvider.ForceResendingToken?) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber, // Phone number to verify
                60, // Timeout duration
                TimeUnit.SECONDS, // Unit of timeout
                this, // Activity (for callback binding)
                mCallbacks!!, // OnVerificationStateChangedCallbacks
                token)             // ForceResendingToken from callbacks
    }

    // [START sign_in_with_phone]
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        mAuth!!.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithCredential:success")

                        val user = task.result.user

                        updateUI(STATE_SIGN_IN_SUCCESS, user)
                    } else {
                        // Sign in failed, display a message and update the UI
                        Log.w(TAG, "signInWithCredential:failure", task.exception)
                        if (task.exception is FirebaseAuthInvalidCredentialsException) {
                            // The verification code entered was invalid
                            mVerificationField!!.error = "Invalid code."
                        }
                        // Update UI
                        updateUI(STATE_SIGN_IN_FAILED)
                    }
                }
    }

    // [END sign_in_with_phone]

    private fun signOut() {
        mAuth!!.signOut()
        updateUI(STATE_INITIALIZED)
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            updateUI(STATE_SIGN_IN_SUCCESS, user)
        } else {
            updateUI(STATE_INITIALIZED)
        }
    }

    private fun updateUI(uiState: Int, cred: PhoneAuthCredential) {
        updateUI(uiState, null, cred)
    }

    private fun updateUI(uiState: Int, user: FirebaseUser? = mAuth!!.currentUser, cred: PhoneAuthCredential? = null) {
        when (uiState) {
            STATE_INITIALIZED -> {
                // Initialized state, show only the phone number field and start button
                enableViews(mStartButton, mPhoneNumberField)
                disableViews(mVerifyButton, mResendButton, mVerificationField)
                mDetailText!!.text = null
            }
            STATE_CODE_SENT -> {
                // Code sent state, show the verification field, the
                enableViews(mVerifyButton, mResendButton, mPhoneNumberField, mVerificationField)
                mDetailText!!.setText(R.string.status_code_sent)
            }
            STATE_VERIFY_FAILED -> {
                // Verification has failed, show all options
                enableViews(mStartButton, mVerifyButton, mResendButton, mPhoneNumberField,
                        mVerificationField)
                mDetailText!!.setText(R.string.status_verification_failed)
            }
            STATE_VERIFY_SUCCESS -> {
                // Verification has succeeded, proceed to firebase sign in
                disableViews(mStartButton, mVerifyButton, mResendButton, mPhoneNumberField,
                        mVerificationField)
                mDetailText!!.setText(R.string.status_verification_succeeded)

                // Set the verification text based on the credential
                if (cred != null) {
                    if (cred.smsCode != null) {
                        mVerificationField!!.setText(cred.smsCode)
                    } else {
                        mVerificationField!!.setText(R.string.instant_validation)
                    }
                }
            }
            STATE_SIGN_IN_FAILED ->
                // No-op, handled by sign-in check
                mDetailText!!.setText(R.string.error_sign_in)
            STATE_SIGN_IN_SUCCESS -> {
            }
        }// Np-op, handled by sign-in check

        if (user == null) {
            // Signed out
            mPhoneNumberViews!!.visibility = View.VISIBLE
            mStatusText!!.setText(R.string.signed_out)
        } else {
            // Signed in
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun enableViews(mStartButton: Button?, mVerifyButton: Button?, mResendButton: Button?, mPhoneNumberField: EditText?, mVerificationField: EditText?) {
        mStartButton?.isEnabled = true
        mVerifyButton?.isEnabled = true
        mResendButton?.isEnabled = true
        mPhoneNumberField?.isEnabled = true
        mVerificationField?.isEnabled = true
    }

    private fun enableViews(mVerifyButton: Button?, mResendButton: Button?, mPhoneNumberField: EditText?, mVerificationField: EditText?) {
        mVerifyButton?.isEnabled = true
        mResendButton?.isEnabled = true
        mPhoneNumberField?.isEnabled = true
        mVerificationField?.isEnabled = true
    }

    private fun enableViews(mStartButton: Button?, mPhoneNumberField: EditText?) {
        mStartButton?.isEnabled = true
        mPhoneNumberField?.isEnabled = true
    }

    private fun disableViews(mVerifyButton: Button?, mResendButton: Button?, mVerificationField: Button?, mPhoneNumberField: EditText?, mVerificationField1: EditText?) {
        mVerifyButton?.isEnabled = false
        mResendButton?.isEnabled = false
        mVerificationField?.isEnabled = false
        mPhoneNumberField?.isEnabled = false
        mVerificationField1?.isEnabled = false
    }

    private fun disableViews(mVerifyButton: Button?, mResendButton: Button?, mVerificationField: EditText?) {
        mVerifyButton?.isEnabled = false
        mResendButton?.isEnabled = false
        mVerificationField?.isEnabled = false
    }

    private fun validatePhoneNumber(): Boolean {
        val phoneNumber = mPhoneNumberField!!.text.toString()
        if (TextUtils.isEmpty(phoneNumber)) {
            mPhoneNumberField!!.error = "Invalid phone number."
            return false
        }

        return true
    }

    /*private fun enableViews(vararg views: View) {
        for (v in views) {
            v.isEnabled = true
        }
    }

    private fun disableViews(vararg views: View) {
        for (v in views) {
            v.isEnabled = false
        }
    }*/

    companion object {

        private val TAG = "PhoneAuthActivity"

        private val KEY_VERIFY_IN_PROGRESS = "key_verify_in_progress"

        private val STATE_INITIALIZED = 1
        private val STATE_CODE_SENT = 2
        private val STATE_VERIFY_FAILED = 3
        private val STATE_VERIFY_SUCCESS = 4
        private val STATE_SIGN_IN_FAILED = 5
        private val STATE_SIGN_IN_SUCCESS = 6
    }
}
