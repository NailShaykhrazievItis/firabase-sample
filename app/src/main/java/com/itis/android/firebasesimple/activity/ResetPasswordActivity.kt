package com.itis.android.firebasesimple.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

import com.google.firebase.auth.FirebaseAuth
import com.itis.android.firebasesimple.R

class ResetPasswordActivity : AppCompatActivity() {

    private var etEmail: EditText? = null

    private var btnResetPassword: Button? = null
    private var btnBack: Button? = null

    private var firebaseAuth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        firebaseAuth = FirebaseAuth.getInstance()

        initFields()
        initClickListeners()
    }

    private fun initClickListeners() {
        btnBack!!.setOnClickListener { v -> finish() }

        btnResetPassword!!.setOnClickListener { v ->
            val email = etEmail!!.text.toString().trim { it <= ' ' }
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(this@ResetPasswordActivity, getString(R.string.error_email), Toast.LENGTH_SHORT)
                        .show()
            } else {
                firebaseAuth!!.sendPasswordResetEmail(email)
                        .addOnCompleteListener { command ->
                            Toast.makeText(this@ResetPasswordActivity, getString(R.string.check_email),
                                    Toast.LENGTH_SHORT).show()
                        }
            }
        }
    }

    private fun initFields() {
        etEmail = findViewById(R.id.email)
        btnResetPassword = findViewById(R.id.btn_reset)
        btnBack = findViewById(R.id.btn_back)
    }
}
