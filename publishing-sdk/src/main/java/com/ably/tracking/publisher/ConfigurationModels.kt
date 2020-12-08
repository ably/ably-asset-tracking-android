package com.ably.tracking.publisher

import com.ably.tracking.Resolution
import io.ably.lib.realtime.ConnectionStateListener

// TODO: data class will be interface, with default implementations as data class for convenience
// TODO: make sure all this works from Java user perspective

data class MapConfiguration(val apiKey: String)

/**
 * Defines the strategy by which the various [ResolutionRequest]s and preferences are translated by [Publisher]
 * instances into a target [Resolution].
 */
interface ResolutionPolicy {
    /**
     * Determine a target resolution from a set of requested resolutions.
     *
     * The set of requested resolutions may be empty.
     */
    fun resolve(requests: Set<ResolutionRequest>): Resolution
}

/**
 * A request for a tracking [Resolution] for a [Trackable] object, where the request [Origin] is known.
 */
interface ResolutionRequest {
    /**
     * The source of a [resolution] request for a [trackable] object.
     */
    enum class Origin {
        /**
         * Configured by the local application.
         */
        LOCAL,

        /**
         * Received from a remote application, where that remote application is a subscriber.
         */
        SUBSCRIBER,
    }

    /**
     * The resolution being requested.
     */
    val resolution: Resolution

    /**
     * The object being tracked.
     */
    val trackable: Trackable

    /**
     * The source of the request.
     */
    val origin: Origin
}

data class Destination(
    val latitude: Double,
    val longitude: Double
)

data class Trackable(
    val id: String,
    val metadata: String? = null,
    val destination: Destination? = null,
    val resolution: Resolution? = null
)

data class TransportationMode(val TBC: String)

// TODO - probably should be removed in the final version
// https://github.com/ably/ably-asset-tracking-android/issues/19
data class DebugConfiguration(
    val ablyStateChangeListener: ((ConnectionStateListener.ConnectionStateChange) -> Unit)? = null,
    val locationSource: LocationSource? = null,
    val locationHistoryReadyListener: ((String) -> Unit)? = null
)

sealed class LocationSource
data class LocationSourceAbly(val simulationChannelName: String) : LocationSource()
data class LocationSourceRaw(val historyData: String) : LocationSource()
