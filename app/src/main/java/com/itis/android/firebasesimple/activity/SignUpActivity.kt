package com.itis.android.firebasesimple.activity

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.design.widget.TextInputLayout
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast

import com.google.firebase.auth.FirebaseAuth
import com.itis.android.firebasesimple.R
import com.itis.android.firebasesimple.utils.SoftKeyboard

/**
 * Created by Nail Shaykhraziev on 02.04.2018.
 */
class SignUpActivity : AppCompatActivity() {

    private var tiEmail: TextInputLayout? = null
    private var tiPassword: TextInputLayout? = null
    private var etEmail: EditText? = null
    private var etPassword: EditText? = null
    private var btnSignIn: Button? = null
    private var btnSignUp: Button? = null
    private var progressBar: ProgressBar? = null
    private var container: View? = null

    private var auth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        //Get Firebase auth instance
        auth = FirebaseAuth.getInstance()

        initFields()

        initClickListeners()

        initTextListeners()
    }

    override fun onResume() {
        super.onResume()
        progressBar!!.visibility = View.GONE
    }

    private fun initFields() {
        container = findViewById(R.id.container)
        btnSignIn = findViewById(R.id.btn_to_signin)
        btnSignUp = findViewById(R.id.btn_signup)
        tiEmail = findViewById(R.id.ti_email)
        tiPassword = findViewById(R.id.ti_password)
        etEmail = findViewById(R.id.email)
        etPassword = findViewById(R.id.password)
        progressBar = findViewById(R.id.progressBar)
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

    private fun initClickListeners() {
        btnSignIn!!.setOnClickListener { v -> finish() }

        btnSignUp!!.setOnClickListener { v ->
            val email = etEmail!!.text.toString().trim { it <= ' ' }
            val password = etPassword!!.text.toString().trim { it <= ' ' }
            if (TextUtils.isEmpty(email)) {
                tiEmail!!.error = getString(R.string.error_email)
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(password)) {
                tiPassword!!.error = getString(R.string.error_pass)
                return@setOnClickListener
            }
            if (password.length < 6) {
                tiPassword!!.error = getString(R.string.error_pass_length)
                return@setOnClickListener
            }
            progressBar!!.visibility = View.VISIBLE
            SoftKeyboard.hide(container!!)
            //create user
            auth!!.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this@SignUpActivity) { task ->
                        Toast.makeText(this@SignUpActivity,
                                "createUserWithEmail:onComplete:" + task.isSuccessful, Toast.LENGTH_SHORT).show()
                        progressBar!!.visibility = View.GONE
                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful) {
                            Snackbar.make(container!!, "Authentication failed." + task.exception!!,
                                    Snackbar.LENGTH_SHORT).show()
                        } else {
                            startActivity(Intent(this@SignUpActivity, MainActivity::class.java))
                            finish()
                        }
                    }
        }
    }
}
