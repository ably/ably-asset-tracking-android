package com.ably.tracking.publisher

import io.ably.lib.realtime.ConnectionStateListener

data class AblyConfiguration(val apiKey: String, val clientId: String)

data class MapConfiguration(val apiKey: String)

data class LogConfiguration(val enabled: Boolean) // TODO - specify config

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
