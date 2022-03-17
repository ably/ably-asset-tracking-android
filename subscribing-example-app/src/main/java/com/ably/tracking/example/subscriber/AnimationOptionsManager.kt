package com.ably.tracking.example.subscriber

import android.content.Context
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog

class AnimationOptionsManager {
    var showRawMarker: Boolean = true
    var showRawAccuracy: Boolean = true
    var showEnhancedMarker: Boolean = true
    var showEnhancedAccuracy: Boolean = true
    var animateCameraMovement: Boolean = true
    var onOptionsChanged: (() -> Unit)? = null

    fun showAnimationOptionsDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle(R.string.animation_options_dialog_title)
            .setMultiChoiceItems(
                createAnimationOptionNames(context),
                createCheckedAnimationOptionsArray()
            ) { _, which, isChecked ->
                when (AnimationOption.fromPosition(which)) {
                    AnimationOption.RAW_MARKER -> showRawMarker = isChecked
                    AnimationOption.RAW_ACCURACY -> showRawAccuracy = isChecked
                    AnimationOption.ENHANCED_MARKER -> showEnhancedMarker = isChecked
                    AnimationOption.ENHANCED_ACCURACY -> showEnhancedAccuracy = isChecked
                    AnimationOption.CAMERA_ANIMATION -> animateCameraMovement = isChecked
                }
            }
            .setPositiveButton(R.string.dialog_ok_button, null)
            .setOnDismissListener { onOptionsChanged?.invoke() }
            .show()
    }

    private fun createAnimationOptionNames(context: Context) =
        AnimationOption.values()
            .sortedBy { it.position }
            .map { context.getString(it.titleResourceId) }
            .toTypedArray()

    private fun createCheckedAnimationOptionsArray() =
        AnimationOption.values()
            .sortedBy { it.position }
            .map {
                when (it) {
                    AnimationOption.RAW_MARKER -> showRawMarker
                    AnimationOption.RAW_ACCURACY -> showRawAccuracy
                    AnimationOption.ENHANCED_MARKER -> showEnhancedMarker
                    AnimationOption.ENHANCED_ACCURACY -> showEnhancedAccuracy
                    AnimationOption.CAMERA_ANIMATION -> animateCameraMovement
                }
            }
            .toBooleanArray()

    private enum class AnimationOption(@StringRes val titleResourceId: Int, val position: Int) {
        RAW_MARKER(R.string.animation_option_show_raw_marker_title, 0),
        RAW_ACCURACY(R.string.animation_option_show_raw_accuracy_title, 1),
        ENHANCED_MARKER(R.string.animation_option_show_enhanced_marker_title, 2),
        ENHANCED_ACCURACY(R.string.animation_option_show_enhanced_accuracy_title, 3),
        CAMERA_ANIMATION(R.string.animation_option_animate_camera_movement_title, 4);

        companion object {
            fun fromPosition(position: Int): AnimationOption {
                return values().find { it.position == position }
                    ?: throw Exception("Can't find the animation option with position $position")
            }
        }
    }
}
