package com.itis.android.firebasesimple.activity

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.itis.android.firebasesimple.R

class RestorePassActivity : AppCompatActivity() {

    private var resetPassEditText: EditText? = null
    private var resetPassButton: Button? = null
    private var firebaseAuth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restore_pass)

        resetPassEditText = findViewById(R.id.edt_reset_pass)
        resetPassButton = findViewById(R.id.btn_reset_pass)
        firebaseAuth = FirebaseAuth.getInstance()

        resetPassButton!!.setOnClickListener {
            val email = resetPassEditText!!.text.toString().trim { it <= ' ' }

            if (TextUtils.isEmpty(email)) {
                resetPassEditText!!.error = "Please enter your email"
            } else {
                firebaseAuth!!.sendPasswordResetEmail(email).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
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
