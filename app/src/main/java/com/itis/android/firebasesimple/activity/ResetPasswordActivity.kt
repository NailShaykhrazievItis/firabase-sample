package com.itis.android.firebasesimple.activity

/**
 * Created by Ruslan on 08.04.2018.
 */

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.view.View
import android.widget.Toast

import com.google.firebase.auth.FirebaseAuth
import com.itis.android.firebasesimple.R
import kotlinx.android.synthetic.main.activity_reset_password.*

class ResetPasswordActivity : AppCompatActivity() {

    private var mAuth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        mAuth = FirebaseAuth.getInstance()

        btn_reset_password.setOnClickListener(View.OnClickListener {
            val email = email.text.toString().trim { it <= ' ' }

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(applicationContext, getString(R.string.enter_email), Toast.LENGTH_SHORT).show()
                return@OnClickListener
            }

            mAuth?.sendPasswordResetEmail(email)?.
                    addOnCompleteListener {
                        if (it.isSuccessful) {
                            Toast.makeText(this@ResetPasswordActivity, getString(R.string.check_email), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@ResetPasswordActivity, getString(R.string.fail_send_email), Toast.LENGTH_SHORT).show()
                        }
                    }
        })

        btn_back.setOnClickListener { finish() }
    }
}