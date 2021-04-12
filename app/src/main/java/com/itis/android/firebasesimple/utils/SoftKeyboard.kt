package com.itis.android.firebasesimple.utils

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

object SoftKeyboard {

    fun hide(view: View?) {
        view?.context?.inputMethodManager()?.run {
            hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun Context.inputMethodManager(): InputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
}
