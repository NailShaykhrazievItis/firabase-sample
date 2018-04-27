package com.itis.android.firebasesimple.utils

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

/**
 * Created by Nail Shaykhraziev on 02.04.2018.
 */
object SoftKeyboard {

    fun hide(view: View) {
        if (view.context != null) {
            val imm = view.context
                    .getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm!!.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}
