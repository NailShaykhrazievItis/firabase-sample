package com.itis.android.firebasesimple.utils;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

/**
 * Created by Nail Shaykhraziev on 02.04.2018.
 */
public class SoftKeyboard {

    public static void hide(final View view) {
        if (view.getContext() != null) {
            InputMethodManager imm = (InputMethodManager) view.getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
