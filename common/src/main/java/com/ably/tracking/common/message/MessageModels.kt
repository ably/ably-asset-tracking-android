package com.ably.tracking.common.message

import com.ably.tracking.annotations.Shared
import com.google.gson.annotations.SerializedName

object GeoJsonTypes {
    const val FEATURE = "Feature"
    const val POINT = "Point"
}

const val GEOMETRY_LONG_INDEX = 0
const val GEOMETRY_LAT_INDEX = 1
const val GEOMETRY_ALT_INDEX = 2

fun LocationMessage.synopsis(): String =
    "[time:${properties.time}; lon:${geometry.coordinates[GEOMETRY_LONG_INDEX]} lat:${geometry.coordinates[GEOMETRY_LAT_INDEX]}; brg:${properties.bearing}]"

@Shared
data class PresenceDataMessage(
    @SerializedName("type") val type: String?,
    @SerializedName("resolution") val resolution: ResolutionMessage? = null,
    @SerializedName("rawLocations") val rawLocations: Boolean? = null,
)

@Shared
data class ResolutionMessage(
    @SerializedName("accuracy") val accuracy: AccuracyMessage,
    @SerializedName("desiredInterval") val desiredInterval: Long,
    @SerializedName("minimumDisplacement") val minimumDisplacement: Double
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
    @SerializedName("location") val location: LocationMessage,
    @SerializedName("skippedLocations") val skippedLocations: List<LocationMessage>,
    @SerializedName("intermediateLocations") val intermediateLocations: List<LocationMessage>,
    @SerializedName("type") val type: LocationUpdateTypeMessage
)

@Shared
enum class LocationUpdateTypeMessage {
    @SerializedName("PREDICTED")
    PREDICTED,

    @SerializedName("ACTUAL")
    ACTUAL,
}

@Shared
data class LocationUpdateMessage(
    @SerializedName("location") val location: LocationMessage,
    @SerializedName("skippedLocations") val skippedLocations: List<LocationMessage>,
)

@Shared
data class LocationMessage(
    @SerializedName("type") val type: String,
    @SerializedName("geometry") val geometry: LocationGeometry,
    @SerializedName("properties") val properties: LocationProperties
)

@Shared
data class LocationGeometry(
    @SerializedName("type") val type: String,
    @SerializedName("coordinates") val coordinates: List<Double>
)

@Shared
data class LocationProperties(
    @SerializedName("accuracyHorizontal") val accuracyHorizontal: Float,
    @SerializedName("bearing") val bearing: Float,
    @SerializedName("speed") val speed: Float,
    @SerializedName("time") val time: Double
)
