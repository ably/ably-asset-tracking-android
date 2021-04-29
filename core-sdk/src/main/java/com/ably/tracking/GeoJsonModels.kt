package com.ably.tracking

// WARNING: Don't add fields to this class because they will be serialized and present in JSON
@Dto
data class GeoJsonMessage(
    val type: String,
    val geometry: GeoJsonGeometry,
    val properties: GeoJsonProperties
)

@Dto
data class GeoJsonGeometry(val type: String, val coordinates: List<Double>)

@Dto
data class GeoJsonProperties(
    val accuracyHorizontal: Float,
    val bearing: Float,
    val speed: Float,
    val time: Double
)
