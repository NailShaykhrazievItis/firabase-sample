package com.itis.android.firebasesimple.activity

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.itis.android.firebasesimple.R
import com.itis.android.firebasesimple.utils.SoftKeyboard
import kotlinx.android.synthetic.main.activity_sign_up.*

class SignUpActivity : AppCompatActivity() {

    private var auth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        //Get Firebase auth instance
        auth = FirebaseAuth.getInstance()

        initClickListeners()
        initTextListeners()
    }

    override fun onResume() {
        super.onResume()
        progressBar.visibility = View.GONE
    }

    private fun initTextListeners() {
        email.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                ti_email.error = null
            }
        })
        password.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                ti_password.error = null
            }
        })
    }

    private fun initClickListeners() {
        btn_to_signin.setOnClickListener { finish() }
        btn_auth_with_phone.setOnClickListener {
            startActivity(Intent(this@SignUpActivity, PhoneAuthActivity::class.java))
        }
        btn_signup.setOnClickListener {
            val email = email.text.toString().trim { it <= ' ' }
            val password = password.text.toString().trim { it <= ' ' }
            if (TextUtils.isEmpty(email)) {
                ti_email.error = getString(R.string.error_email)
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(password)) {
                ti_password.error = getString(R.string.error_pass)
                return@setOnClickListener
            }
            if (password.length < 6) {
                ti_password.error = getString(R.string.error_pass_length)
                return@setOnClickListener
            }
            progressBar.visibility = View.VISIBLE
            SoftKeyboard.hide(container)
            //create user
            auth?.createUserWithEmailAndPassword(email, password)
                    ?.addOnCompleteListener(this@SignUpActivity) { task ->
                        Toast.makeText(this@SignUpActivity,
                                "createUserWithEmail:onComplete:" + task.isSuccessful, Toast.LENGTH_SHORT).show()
                        progressBar.visibility = View.GONE
                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful) {
                            Snackbar.make(container, "Authentication failed." + task.exception!!, Snackbar.LENGTH_SHORT).show()
                        } else {
                            startActivity(Intent(this@SignUpActivity, MainActivity::class.java))
                            finish()
                        }
                    }
        }
    }
}
