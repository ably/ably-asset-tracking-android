package com.ably.tracking

// WARNING: Don't add fields to this class because they will be serialized and present in JSON
data class GeoJsonMessage(
    val type: String,
    val geometry: GeoJsonGeometry,
    val properties: GeoJsonProperties
)

data class GeoJsonGeometry(val type: String, val coordinates: List<Double>)

data class GeoJsonProperties(
    val accuracyHorizontal: Float,
    val altitude: Double,
    val bearing: Float,
    val speed: Float,
    val time: Double
)
