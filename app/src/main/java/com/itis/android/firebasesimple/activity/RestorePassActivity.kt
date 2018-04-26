package com.itis.android.firebasesimple.activity

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.itis.android.firebasesimple.R
import kotlinx.android.synthetic.main.activity_restore_pass.*

class RestorePassActivity : AppCompatActivity() {

    private var firebaseAuth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restore_pass)

        firebaseAuth = FirebaseAuth.getInstance()

        btn_reset_pass.setOnClickListener {
            val email = edt_reset_pass.text.toString().trim { it <= ' ' }

            if (TextUtils.isEmpty(email)) {
                edt_reset_pass.error = "Please enter your email"
            } else {
                firebaseAuth?.sendPasswordResetEmail(email)?.addOnCompleteListener {
                    if (it.isSuccessful) {
                        Toast.makeText(applicationContext,
                                getString(R.string.reset_password_complete_msg), Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@RestorePassActivity, SignInActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(applicationContext, getString(R.string.error), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
