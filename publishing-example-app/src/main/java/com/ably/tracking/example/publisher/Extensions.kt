package com.ably.tracking.example.publisher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat

private const val NO_FLAGS = 0

fun Context.hideKeyboard(view: View) {
    (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(
        view.windowToken,
        NO_FLAGS
    )
}

fun Context.showShortToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Context.showLongToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

fun Context.showShortToast(@StringRes stringResourceId: Int) {
    Toast.makeText(this, stringResourceId, Toast.LENGTH_SHORT).show()
}

fun Context.showLongToast(@StringRes stringResourceId: Int) {
    Toast.makeText(this, stringResourceId, Toast.LENGTH_LONG).show()
}

fun Button.hideText() {
    textScaleX = 0f
}

fun Button.showText() {
    textScaleX = 1f
}

fun Context.createBitmapFromVectorDrawable(@DrawableRes id: Int): Bitmap? =
    ContextCompat.getDrawable(this, id)?.let { drawable ->
        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).let {
            drawable.setBounds(0, 0, it.width, it.height)
            drawable.draw(it)
        }
        bitmap
    }
