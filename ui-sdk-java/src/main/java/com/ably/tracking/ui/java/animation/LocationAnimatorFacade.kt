package com.ably.tracking.ui.java.animation

import com.ably.tracking.ui.animation.LocationAnimator
import com.ably.tracking.ui.animation.Position

/**
 * Methods provided for those using the [LocationAnimator] from Java code (Java 1.8 or higher).
 *
 * Kotlin users will generally prefer to directly use the interfaces offered by [LocationAnimator].
 */
interface LocationAnimatorFacade : LocationAnimator {
    /**
     * Adds a handler to be notified when a new map marker position is available.
     * The position of the map marker should be changed each time a new value arrives in this flow.
     *
     * @param listener The listening function to be notified.
     */
    fun addPositionListener(listener: PositionListener)

    /**
     * Adds a handler to be notified when a new camera position is available.
     * It helps to follow the animation progress from [addPositionListener].
     * The position of the camera can be changed each time a new value arrives in this flow.
     *
     * @param listener The listening function to be notified.
     */
    fun addCameraPositionListener(listener: PositionListener)
}

/**
 * Defines an interface, to be implemented in Java code utilising the Ably Asset Tracking SDKs, allowing that code to
 * handle events containing position changes.
 */
interface PositionListener {
    fun onPositionChanged(position: Position)
}
