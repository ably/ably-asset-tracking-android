package com.ably.tracking.example.publisher

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

private const val NO_FLAGS = 0

fun Context.hideKeyboard(view: View) {
    (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(
        view.windowToken,
        NO_FLAGS
    )
}
