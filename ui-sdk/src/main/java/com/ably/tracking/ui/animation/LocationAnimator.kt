package com.ably.tracking.ui.animation

import com.ably.tracking.LocationUpdate
import kotlinx.coroutines.flow.Flow

/**
 * Extension for the [com.ably.tracking.subscriber.Subscriber] that enables smooth location updates animation.
 */
interface LocationAnimator {
    /**
     * The flow of subsequent map marker positions that create the animation.
     * The position of the map marker should be changed each time a new value arrives in this flow.
     */
    val positionsFlow: Flow<Position>
        @JvmSynthetic get

    /**
     * The flow of subsequent camera positions that helps to follow the animation progress from [positionsFlow].
     * The position of the camera can be changed each time a new value arrives in this flow.
     */
    val cameraPositionsFlow: Flow<Position>
        @JvmSynthetic get

    /**
     * Queues the location update for the animation.
     * This should be called each time a new location update is received from the [com.ably.tracking.subscriber.Subscriber].
     *
     * @param locationUpdate The newest location update received from the Subscriber SDK.
     * @param expectedIntervalBetweenLocationUpdatesInMilliseconds The expected interval of location updates in milliseconds.
     */
    fun animateLocationUpdate(
        locationUpdate: LocationUpdate,
        expectedIntervalBetweenLocationUpdatesInMilliseconds: Long,
    )

    /**
     * Stops and cancels any ongoing animations.
     * After stopping the [LocationAnimator] instance should not be used anymore.
     */
    fun stop()
}

/**
 * Represents a position on the map with additional properties required for a map marker animation.
 */
data class Position(
    /**
     * The latitude of the location in degrees.
     */
    val latitude: Double,

    /**
     * The longitude of the location in degrees.
     */
    val longitude: Double,

    /**
     * The bearing of the location in degrees.
     */
    val bearing: Float,

    /**
     * The accuracy of the location in meters.
     */
    val accuracy: Float
)
