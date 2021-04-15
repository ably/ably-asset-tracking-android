package com.ably.tracking.example.publisher

import android.text.InputType
import androidx.preference.EditTextPreference

fun EditTextPreference.setIntNumberInputType() {
    setOnBindEditTextListener {
        it.inputType = InputType.TYPE_CLASS_NUMBER
    }
}

fun EditTextPreference.setFloatNumberInputType() {
    setOnBindEditTextListener {
        it.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
    }
}
