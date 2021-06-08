package com.ably.tracking.common

const val MILLISECONDS_PER_SECOND = 1000

const val MILLISECONDS_PER_MINUTE = MILLISECONDS_PER_SECOND * 60

const val METERS_PER_KILOMETER = 1000

object ChannelNames {
    /**
     * This is a metachannel, which will be defined across the Ably infrastructure.
     *
     * **Note**: Simply using `tracking` as the namespace prefix wouldn't be enough as,
     * in the wider Ably context, `tracking` can mean many things. Whereas,
     * `asset-tracking` specifies a product.
     */
    const val METADATA_TRIP = "[meta]asset-tracking:trip-lifecycle"
}

/**
 * The trip event naming approach (e.g. `trip.start`) is similar to what we use in
 * lifecycle events on Ably metachannels (see
 * [Lifecycle Events](https://ably.com/documentation/realtime/metachannels#lifecycle-events)).
 *
 * This way we align the metachannel message format.
 */
object EventNames {
    const val ENHANCED = "enhanced"
    const val TRIP_START = "trip.start"
    const val TRIP_END = "trip.end"
}

object ClientTypes {
    const val SUBSCRIBER = "SUBSCRIBER"
    const val PUBLISHER = "PUBLISHER"
}
