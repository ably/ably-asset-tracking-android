package com.ably.tracking.publisher

import com.ably.tracking.Resolution
import io.ably.lib.realtime.ConnectionStateListener

// TODO: data class will be interface, with default implementations as data class for convenience
// TODO: make sure all this works from Java user perspective

data class MapConfiguration(val apiKey: String)

interface ResolutionPolicy {
    fun resolve(resolutions: Set<Resolution>): Resolution
}

data class Destination(
    val latitude: Double,
    val longitude: Double
)

data class Trackable(
    val id: String,
    val metadata: String? = null,
    val destination: Destination? = null,
    val targetResolution: Resolution? = null
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
