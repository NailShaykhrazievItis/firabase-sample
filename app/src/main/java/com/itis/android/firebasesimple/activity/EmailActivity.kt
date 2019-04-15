package com.itis.android.firebasesimple.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import com.google.firebase.auth.FirebaseAuth
import com.itis.android.firebasesimple.R
import kotlinx.android.synthetic.main.activity_email.*

class EmailActivity : AppCompatActivity()  {

    private var firebaseAuth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email)

        firebaseAuth = FirebaseAuth.getInstance()
        initClickListener()
    }

    private fun initClickListener(){
        btn_ok.setOnClickListener{
            val email = et_email.text.toString()

            if (TextUtils.isEmpty(email)) {
                textinput_error_email.error = getString(R.string.error_email)
                return@setOnClickListener
            }

            firebaseAuth?.sendPasswordResetEmail(email)
        }
    }
}
