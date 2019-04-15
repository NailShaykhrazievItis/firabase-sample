package com.itis.android.firebasesimple.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import com.google.firebase.auth.FirebaseAuth
import com.itis.android.firebasesimple.R
import kotlinx.android.synthetic.main.activity_new_password.*

class PasswordResetActivity : AppCompatActivity() {

    private var firebaseAuth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_password)

        firebaseAuth = FirebaseAuth.getInstance()
        initClickListener()
    }

    private fun initClickListener(){
        btn_reset_passw.setOnClickListener {
            val code = et_reset_code.text.toString()
            val new_passw = et_new_password.text.toString()

            if (TextUtils.isEmpty(code)) {
                error_reset_code.error = getString(R.string.error_email)
                return@setOnClickListener
            }

            if (TextUtils.isEmpty(new_passw)) {
                error_new_passw.error = getString(R.string.error_pass)
                return@setOnClickListener
            }
            firebaseAuth?.confirmPasswordReset(code, new_passw)
        }
    }
}
