package com.ably.tracking.common

const val MILLISECONDS_PER_SECOND = 1000

const val MILLISECONDS_PER_MINUTE = MILLISECONDS_PER_SECOND * 60

const val METERS_PER_KILOMETER = 1000

/**
 * The trip event naming approach (e.g. `trip.start`) is similar to what we use in
 * lifecycle events on Ably metachannels (see
 * [Lifecycle Events](https://ably.com/documentation/realtime/metachannels#lifecycle-events)).
 *
 * This way we align the metachannel message format.
 */
object EventNames {
    const val ENHANCED = "enhanced"
    const val RAW = "raw"
    const val RESOLUTION = "resolution"
}

object ClientTypes {
    const val SUBSCRIBER = "SUBSCRIBER"
    const val PUBLISHER = "PUBLISHER"
}
