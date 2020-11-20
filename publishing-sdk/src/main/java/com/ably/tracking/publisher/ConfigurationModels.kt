package com.ably.tracking.publisher

data class AblyConfiguration(val apiKey: String, val clientId: String)

data class MapConfiguration(val apiKey: String)

data class LogConfiguration(val enabled: Boolean) // TODO - specify config

data class BatteryConfiguration(val placeholder: String) // TODO - specify config
