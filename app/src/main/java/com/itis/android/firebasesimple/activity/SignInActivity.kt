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
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast

import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.itis.android.firebasesimple.R
import com.itis.android.firebasesimple.utils.SoftKeyboard

class SignInActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener {

    private var googleSignInButton: SignInButton? = null
    private var tiEmail: TextInputLayout? = null
    private var tiPassword: TextInputLayout? = null
    private var etEmail: EditText? = null
    private var etPassword: EditText? = null
    private var btnSignIn: Button? = null
    private var btnSignUp: Button? = null
    private var btnResetPassword: Button? = null
    private var buttonSignInPhone: Button? = null
    private var progressBar: ProgressBar? = null
    private var container: View? = null

    private var googleApiClient: GoogleApiClient? = null
    private var firebaseAuth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        initGoogleAuth()

        // Initialize FirebaseAuth
        firebaseAuth = FirebaseAuth.getInstance()

        if (firebaseAuth!!.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        initFields()
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
                firebaseAuthWithGoogle(account!!)
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
        Toast.makeText(this, "Google Play Services error.", Toast.LENGTH_SHORT).show()
    }

    private fun initClickListeners() {
        btnSignUp!!.setOnClickListener { v -> startActivity(Intent(this, SignUpActivity::class.java)) }

        btnResetPassword!!.setOnClickListener { v -> startActivity(Intent(this, PasswordResetActivity::class.java)) }

        btnSignIn!!.setOnClickListener { v ->
            val email = etEmail!!.text.toString()
            val password = etPassword!!.text.toString()

            if (TextUtils.isEmpty(email)) {
                tiEmail!!.error = getString(R.string.error_email)
                return@setOnClickListener
            }

            if (TextUtils.isEmpty(password)) {
                tiPassword!!.error = getString(R.string.error_pass)
                return@setOnClickListener
            }

            progressBar!!.visibility = View.VISIBLE
            SoftKeyboard.hide(container!!)

            //authenticate user
            firebaseAuth!!.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this@SignInActivity) { task ->
                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        progressBar!!.visibility = View.GONE
                        if (!task.isSuccessful) {
                            // there was an error
                            if (password.length < 6) {
                                tiPassword!!.error = getString(R.string.error_pass_length)
                            } else {
                                Snackbar.make(container!!, R.string.error_signin, Snackbar.LENGTH_SHORT).show()
                            }
                        } else {
                            val intent = Intent(this@SignInActivity, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }
        }

        buttonSignInPhone!!.setOnClickListener { v ->
            startActivity(Intent(this, SignInWithPhoneActivity::class.java))
        }
    }

    private fun initFields() {
        container = findViewById(R.id.container)
        btnSignIn = findViewById(R.id.btn_login)
        btnSignUp = findViewById(R.id.btn_to_signup)
        tiEmail = findViewById(R.id.ti_email)
        tiPassword = findViewById(R.id.ti_password)
        etEmail = findViewById(R.id.email)
        etPassword = findViewById(R.id.password)
        progressBar = findViewById(R.id.progressBar)
        btnResetPassword = findViewById(R.id.btn_reset_password)
        buttonSignInPhone = findViewById(R.id.btn_login_with_phone)
    }

    private fun initGoogleAuth() {
        // Assign fields
        googleSignInButton = findViewById(R.id.sign_in_button)
        // Set click listeners
        googleSignInButton!!.setOnClickListener { v -> signIn() }
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
            Toast.makeText(this, "Welcome " + user.email!!, Toast.LENGTH_SHORT).show()

            // Go back to the main activity
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.id!!)
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        firebaseAuth!!.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful)
                    // If sign in fails, display a message to the user. If sign in succeeds
                    // the auth state listener will be notified and logic to handle the
                    // signed in user can be handled in the listener.
                    if (!task.isSuccessful) {
                        Log.w(TAG, "signInWithCredential", task.exception)
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
        etEmail!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable) {
                tiEmail!!.error = null
            }
        })
        etPassword!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable) {
                tiPassword!!.error = null
            }
        })
    }

    companion object {

        private val TAG = "SignInActivity"
        private val RC_SIGN_IN = 9001
    }
}
