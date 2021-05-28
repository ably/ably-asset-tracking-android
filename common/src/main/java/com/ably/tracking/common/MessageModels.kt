package com.ably.tracking.common

import com.ably.tracking.annotations.Shared
import com.ably.tracking.GeoJsonMessage
import com.ably.tracking.Resolution
import com.google.gson.annotations.SerializedName

object GeoJsonTypes {
    const val FEATURE = "Feature"
    const val POINT = "Point"
}

const val GEOMETRY_LONG_INDEX = 0
const val GEOMETRY_LAT_INDEX = 1
const val GEOMETRY_ALT_INDEX = 2

fun GeoJsonMessage.synopsis(): String =
    "[time:${properties.time}; lon:${geometry.coordinates[GEOMETRY_LONG_INDEX]} lat:${geometry.coordinates[GEOMETRY_LAT_INDEX]}; brg:${properties.bearing}]"

data class PresenceMessage(val action: PresenceAction, val data: PresenceData, val clientId: String)

enum class PresenceAction {
    PRESENT_OR_ENTER, LEAVE_OR_ABSENT, UPDATE;
}

data class PresenceData(val type: String, val resolution: Resolution? = null)

@Shared
data class PresenceDataMessage(val type: String, val resolution: ResolutionMessage? = null)

@Shared
data class ResolutionMessage(
    val accuracy: AccuracyMessage,
    val desiredInterval: Long,
    val minimumDisplacement: Double
)

@Shared
enum class AccuracyMessage {
    @SerializedName("MINIMUM")
    MINIMUM,

    @SerializedName("LOW")
    LOW,

    @SerializedName("BALANCED")
    BALANCED,

    @SerializedName("HIGH")
    HIGH,

    @SerializedName("MAXIMUM")
    MAXIMUM,
}

@Shared
data class EnhancedLocationUpdateMessage(
    val location: GeoJsonMessage,
    val skippedLocations: List<GeoJsonMessage>,
    val intermediateLocations: List<GeoJsonMessage>,
    val type: LocationUpdateTypeMessage
)

@Shared
enum class LocationUpdateTypeMessage {
    @SerializedName("PREDICTED")
    PREDICTED,

    @SerializedName("ACTUAL")
    ACTUAL,
}
