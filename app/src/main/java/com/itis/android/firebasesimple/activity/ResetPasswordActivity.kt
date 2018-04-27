package com.itis.android.firebasesimple.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.itis.android.firebasesimple.R
import kotlinx.android.synthetic.main.activity_reset_password.*

class ResetPasswordActivity : AppCompatActivity() {

    private var firebaseAuth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        firebaseAuth = FirebaseAuth.getInstance()

        initClickListeners()
    }

    private fun initClickListeners() {
        btn_back!!.setOnClickListener { finish() }

        btn_reset!!.setOnClickListener { v ->
            val email = et_email!!.text.toString().trim { it <= ' ' }
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(this@ResetPasswordActivity, getString(R.string.error_email), Toast.LENGTH_SHORT)
                        .show()
            } else {
                firebaseAuth!!.sendPasswordResetEmail(email)
                        .addOnCompleteListener {
                            Toast.makeText(this@ResetPasswordActivity, getString(R.string.check_email),
                                    Toast.LENGTH_SHORT).show()
                        }
            }
        }
    }
}
