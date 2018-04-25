package com.itis.android.firebasesimple.activity

import android.support.design.widget.Snackbar
import android.support.design.widget.TextInputLayout
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.itis.android.firebasesimple.R
import com.itis.android.firebasesimple.utils.SoftKeyboard
import kotlinx.android.synthetic.main.activity_password_reset.btn_send_SMS
import kotlinx.android.synthetic.main.activity_password_reset.btn_send_email
import kotlinx.android.synthetic.main.activity_password_reset.container
import kotlinx.android.synthetic.main.activity_password_reset.et_email
import kotlinx.android.synthetic.main.activity_password_reset.et_phone
import kotlinx.android.synthetic.main.activity_password_reset.progressBar
import kotlinx.android.synthetic.main.activity_password_reset.ti_email
import kotlinx.android.synthetic.main.activity_password_reset.ti_phone

class PasswordResetActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_reset)

        initClickListeners()
        initTextListeners()
    }

    override fun onResume() {
        super.onResume()
        progressBar.visibility = View.GONE
    }

    private fun initClickListeners() {
        btn_send_email.setOnClickListener {
            val email = et_email.text.toString().trim { it <= ' ' }
            if (TextUtils.isEmpty(email)) {
                ti_email.error = getString(R.string.error_email)
                return@setOnClickListener
            }
            progressBar.visibility = View.VISIBLE
            SoftKeyboard.hide(container)

            val auth = FirebaseAuth.getInstance()

            auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            Snackbar.make(container, "E-mail send.", Snackbar.LENGTH_SHORT).show()
                        } else {
                            Snackbar.make(container, "Some problems..." + it.exception, Snackbar
                                    .LENGTH_SHORT)
                        }
                    }
            progressBar.visibility = View.GONE
        }
    }

    private fun initTextListeners() {
        et_email.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable) {
                ti_email.error = null
            }
        })

        et_phone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable) {
                ti_phone.error = null
            }
        })
    }
}
