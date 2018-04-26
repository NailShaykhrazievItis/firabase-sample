package com.itis.android.firebasesimple.activity

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.itis.android.firebasesimple.R
import com.itis.android.firebasesimple.utils.hide
import kotlinx.android.synthetic.main.activity_sign_in.*

class SignInActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener {

    private var googleApiClient: GoogleApiClient? = null
    private var firebaseAuth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        initGoogleAuth()

        // Initialize FirebaseAuth
        firebaseAuth = FirebaseAuth.getInstance()

        if (firebaseAuth?.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        initClickListeners()
        initTextListeners()
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if (result.isSuccess) {
                // Google Sign In was successful, authenticate with Firebase
                val account = result.signInAccount
                if (account != null)
                    firebaseAuthWithGoogle(account)
            } else {
                // Google Sign In failed
                Log.e(TAG, "Google Sign In failed.")
            }
        }
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed:$connectionResult")
        Snackbar.make(container, "Google Play Services error.", Snackbar.LENGTH_SHORT).show()
    }

    private fun initClickListeners() {
        btn_to_signup.setOnClickListener { startActivity(Intent(this@SignInActivity, SignUpActivity::class.java)) }

        btn_reset_password.setOnClickListener { startActivity(Intent(this, ResetPasswordActivity::class.java)) }

        sign_in_phone_number_button.setOnClickListener {
            startActivity(Intent(this@SignInActivity, SignInPhoneNumberActivity::class.java))

            btn_login.setOnClickListener {
                val email = email.text.toString()
                val password = password.text.toString()

                if (TextUtils.isEmpty(email)) {
                    ti_email.error = getString(R.string.error_email)
                }

                if (TextUtils.isEmpty(password)) {
                    ti_password.error = getString(R.string.error_pass)
                }

                progressBar.visibility = View.VISIBLE
                if (container != null)
                    hide(container)

                //authenticate user
                firebaseAuth?.signInWithEmailAndPassword(email, password)
                        ?.addOnCompleteListener(this@SignInActivity) {
                            // If sign in fails, display a message to the user. If sign in succeeds
                            // the auth state listener will be notified and logic to handle the
                            // signed in user can be handled in the listener.
                            progressBar.visibility = View.GONE
                            if (!it.isSuccessful) {
                                // there was an error
                                if (password.length < 6) {
                                    ti_password.error = getString(R.string.error_pass_length)
                                } else {
                                    if (container != null)
                                        Snackbar.make(container, R.string.error_signin, Snackbar.LENGTH_SHORT).show()
                                }
                            } else {
                                val intent = Intent(this@SignInActivity, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                        }
            }
        }
    }

    private fun initGoogleAuth() {
        // Set click listeners
        sign_in_button.setOnClickListener { signIn() }
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        googleApiClient = GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build()
    }

    private fun handleFirebaseAuthResult(authResult: AuthResult?) {
        if (authResult != null) {
            // Welcome the user
            val user = authResult.user
            if (container != null && user != null)
                Snackbar.make(container, "Welcome " + user.email, Snackbar.LENGTH_SHORT).show()

            // Go back to the main activity
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.id)
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        firebaseAuth?.signInWithCredential(credential)
                ?.addOnCompleteListener(this) {
                    Log.d(TAG, "signInWithCredential:onComplete:" + it.isSuccessful)
                    // If sign in fails, display a message to the user. If sign in succeeds
                    // the auth state listener will be notified and logic to handle the
                    // signed in user can be handled in the listener.
                    if (!it.isSuccessful) {
                        Log.w(TAG, "signInWithCredential", it.exception)
                        Toast.makeText(this@SignInActivity, "Authentication failed.",
                                Toast.LENGTH_SHORT).show()
                    } else {
                        startActivity(Intent(this@SignInActivity, MainActivity::class.java))
                        finish()
                    }
                }
    }

    private fun signIn() {
        val signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient)
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun initTextListeners() {
        email.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (email.text.toString().length > 0 && password.text.toString().length > 0) {
                    sign_in_phone_number_button.isEnabled = true
                } else {
                    sign_in_phone_number_button.isEnabled = false
                }
            }

            override fun afterTextChanged(s: Editable) {
                ti_email.error = null
            }
        })
        password.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (email.text.toString().length > 0 && password.text.toString().length > 0) {
                    sign_in_phone_number_button.isEnabled = true
                } else {
                    sign_in_phone_number_button.isEnabled = false
                }
            }

            override fun afterTextChanged(s: Editable) {
                ti_password.error = null
            }
        })
    }

    companion object {

        private val TAG = "SignInActivity"
        private val RC_SIGN_IN = 9001
    }
}