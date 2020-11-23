package com.ably.tracking.publisher

import io.ably.lib.realtime.ConnectionStateListener
import java.io.File

data class AblyConfiguration(val apiKey: String, val clientId: String)

data class MapConfiguration(val apiKey: String)

data class LogConfiguration(val enabled: Boolean) // TODO - specify config

data class BatteryConfiguration(val placeholder: String) // TODO - specify config

// TODO - probably should be removed in the final version
data class DebugConfiguration(
    val ablyStateChangeListener: ((ConnectionStateListener.ConnectionStateChange) -> Unit)? = null,
    val locationConfiguration: LocationConfiguration? = null
)

sealed class LocationConfiguration

data class LocationConfigurationAbly(val simulationChannelName: String) : LocationConfiguration()

data class LocationConfigurationS3(val s3FileName: String, val downloadDir: File) :
    LocationConfiguration()
