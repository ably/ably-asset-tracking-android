package com.ably.tracking.common

import com.ably.tracking.Accuracy
import com.ably.tracking.LocationUpdateType
import com.google.gson.annotations.SerializedName

object GeoJsonTypes {
    const val FEATURE = "Feature"
    const val POINT = "Point"
}

const val GEOMETRY_LONG_INDEX = 0
const val GEOMETRY_LAT_INDEX = 1

data class GeoJsonMessage(
    @SerializedName("type") val type: String,
    @SerializedName("geometry") val geometry: GeoJsonGeometry,
    @SerializedName("properties") val properties: GeoJsonProperties
) {
    // WARNING: Don't add fields to this class because they will be serialized and present in JSON

    fun synopsis(): String =
        "[time:${properties.time}; lon:${geometry.coordinates[GEOMETRY_LONG_INDEX]} lat:${geometry.coordinates[GEOMETRY_LAT_INDEX]}; brg:${properties.bearing}]"
}

data class GeoJsonGeometry(
    @SerializedName("type") val type: String,
    @SerializedName("coordinates") val coordinates: List<Double>
)

data class GeoJsonProperties(
    @SerializedName("accuracyHorizontal") val accuracyHorizontal: Float,
    @SerializedName("altitude") val altitude: Double,
    @SerializedName("bearing") val bearing: Float,
    @SerializedName("speed") val speed: Float,
    @SerializedName("time") val time: Double
)

data class PresenceData(
    @SerializedName("type") val type: String,
    @SerializedName("resolution") val resolutionRequest: ResolutionRequest? = null
)

data class ResolutionRequest(
    @SerializedName("accuracy") val accuracy: Accuracy,
    @SerializedName("desiredInterval") val desiredInterval: Long,
    @SerializedName("minimumDisplacement") val minimumDisplacement: Double
)

data class EnhancedLocationUpdateMessage(
    val location: GeoJsonMessage,
    val intermediateLocations: List<GeoJsonMessage>,
    val type: LocationUpdateType
)
