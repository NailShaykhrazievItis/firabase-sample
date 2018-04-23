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

class PasswordResetActivity : AppCompatActivity() {

    private var tiEmail: TextInputLayout? = null
    private var tiPhone: TextInputLayout? = null
    private var etEmail: EditText? = null
    private var etPhone: EditText? = null
    private var btnSendEmail: Button? = null
    private var btnSendSMS: Button? = null
    private var progressBar: ProgressBar? = null
    private var container: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_reset)

        initFields()
        initClickListeners()
        initTextListeners()
    }

    override fun onResume() {
        super.onResume()
        progressBar!!.visibility = View.GONE
    }

    private fun initFields() {
        tiEmail = findViewById(R.id.ti_email)
        tiPhone = findViewById(R.id.ti_phone)
        etEmail = findViewById(R.id.et_email)
        etPhone = findViewById(R.id.et_phone)
        btnSendEmail = findViewById(R.id.btn_send_email)
        btnSendSMS = findViewById(R.id.btn_send_SMS)
        progressBar = findViewById(R.id.progressBar)
        container = findViewById(R.id.container)
    }

    private fun initClickListeners() {
        btnSendEmail!!.setOnClickListener { v ->
            val email = etEmail!!.text.toString().trim { it <= ' ' }
            if (TextUtils.isEmpty(email)) {
                tiEmail!!.error = getString(R.string.error_email)
                return@setOnClickListener
            }
            progressBar!!.visibility = View.VISIBLE
            SoftKeyboard.hide(container!!)

            val auth = FirebaseAuth.getInstance()

            auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Snackbar.make(container!!, "E-mail send.", Snackbar.LENGTH_SHORT).show()
                        } else {
                            Snackbar.make(container!!, "Some problems..." + task.exception!!, Snackbar
                                    .LENGTH_SHORT)
                        }
                    }
            progressBar!!.visibility = View.GONE
        }

        btnSendSMS!!.setOnClickListener { v -> }
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

        etPhone!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable) {
                tiPhone!!.error = null
            }
        })
    }
}
