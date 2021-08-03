package com.ably.tracking.publisher

import com.ably.tracking.annotations.Shared
import com.google.gson.annotations.SerializedName

// WARNING: Don't add fields to this class because they will be serialized and present in JSON
@Shared
data class GeoJsonMessage(
    @SerializedName("type") val type: String,
    @SerializedName("geometry") val geometry: GeoJsonGeometry,
    @SerializedName("properties") val properties: GeoJsonProperties
)

@Shared
data class GeoJsonGeometry(
    @SerializedName("type") val type: String,
    @SerializedName("coordinates") val coordinates: List<Double>
)

@Shared
data class GeoJsonProperties(
    @SerializedName("accuracyHorizontal") val accuracyHorizontal: Float,
    @SerializedName("bearing") val bearing: Float,
    @SerializedName("speed") val speed: Float,
    @SerializedName("time") val time: Double
)
