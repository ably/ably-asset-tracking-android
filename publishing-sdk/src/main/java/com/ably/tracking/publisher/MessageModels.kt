package com.ably.tracking.publisher

object GeoJsonTypes {
    const val FEATURE = "Feature"
    const val POINT = "Point"
}

const val GEOMETRY_LONG_INDEX = 0
const val GEOMETRY_LAT_INDEX = 1

data class GeoJsonMessage(
    val type: String,
    val geometry: GeoJsonGeometry,
    val properties: GeoJsonProperties
) {
    // WARNING: Don't add fields to this class because they will be serialized and present in JSON

    fun synopsis(): String =
        "[time:${properties.time}; lon:${geometry.coordinates[GEOMETRY_LONG_INDEX]} lat:${geometry.coordinates[GEOMETRY_LAT_INDEX]}; brg:${properties.bearing}]"
}

data class GeoJsonGeometry(val type: String, val coordinates: List<Double>)

data class GeoJsonProperties(
    val accuracyHorizontal: Float,
    val altitude: Double,
    val bearing: Float,
    val speed: Float,
    val time: Double
)
