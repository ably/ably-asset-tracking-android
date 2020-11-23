package com.ably.tracking.publisher

import io.ably.lib.realtime.ConnectionStateListener

data class AblyConfiguration(val apiKey: String, val clientId: String)

data class MapConfiguration(val apiKey: String)

data class LogConfiguration(val enabled: Boolean) // TODO - specify config

data class BatteryConfiguration(val placeholder: String) // TODO - specify config

// TODO - probably should be removed in the final version
data class DebugConfiguration(
    val ablyStateChangeListener: ((ConnectionStateListener.ConnectionStateChange) -> Unit)? = null,
    val locationSource: LocationSource? = null
)

sealed class LocationSource
data class LocationSourceAbly(val simulationChannelName: String) : LocationSource()
data class LocationSourceRaw(val historyData: String) : LocationSource()
