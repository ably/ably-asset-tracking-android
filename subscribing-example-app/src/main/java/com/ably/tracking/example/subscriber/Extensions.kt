package com.ably.tracking.example.subscriber

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button

private const val NO_FLAGS = 0

fun Context.hideKeyboard(view: View) {
    (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(
        view.windowToken,
        NO_FLAGS
    )
}

fun Button.hideText() {
    textScaleX = 0f
}

fun Button.showText() {
    textScaleX = 1f
}
