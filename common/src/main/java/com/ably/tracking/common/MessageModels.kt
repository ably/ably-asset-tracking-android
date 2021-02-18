package com.ably.tracking.common

import com.ably.tracking.GeoJsonMessage
import com.ably.tracking.LocationUpdateType
import com.ably.tracking.Resolution

object GeoJsonTypes {
    const val FEATURE = "Feature"
    const val POINT = "Point"
}

const val GEOMETRY_LONG_INDEX = 0
const val GEOMETRY_LAT_INDEX = 1

fun GeoJsonMessage.synopsis(): String =
    "[time:${properties.time}; lon:${geometry.coordinates[GEOMETRY_LONG_INDEX]} lat:${geometry.coordinates[GEOMETRY_LAT_INDEX]}; brg:${properties.bearing}]"

data class PresenceMessage(val action: PresenceAction, val data: PresenceData, val clientId: String)

enum class PresenceAction {
    PRESENT_OR_ENTER, LEAVE_OR_ABSENT, UPDATE;
}

data class PresenceData(val type: String, val resolution: Resolution? = null)

data class EnhancedLocationUpdateMessage(
    val location: GeoJsonMessage,
    val intermediateLocations: List<GeoJsonMessage>,
    val type: LocationUpdateType
)
