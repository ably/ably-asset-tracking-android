package com.ably.tracking

import android.location.Location
import com.ably.tracking.common.GeoJsonMessage
import com.ably.tracking.common.toGeoJson
import com.ably.tracking.common.toLocation
import com.google.gson.Gson
import io.ably.lib.types.Message

open class LocationUpdate(val location: Location)

class EnhancedLocationUpdate(
    location: Location,
    val predictedLocations: List<Location>,
    val type: LocationUpdateType
) : LocationUpdate(location)

enum class LocationUpdateType {
    PREDICTED, ACTUAL
}

internal class EnhancedLocationUpdateMessage(
    val location: GeoJsonMessage,
    val predictedLocations: List<GeoJsonMessage>,
    val type: LocationUpdateType
)

fun EnhancedLocationUpdate.toJson(gson: Gson): String =
    gson.toJson(EnhancedLocationUpdateMessage(location.toGeoJson(), predictedLocations.map { it.toGeoJson() }, type))

fun Message.getEnhancedLocationUpdate(gson: Gson): EnhancedLocationUpdate =
    gson.fromJson(data as String, EnhancedLocationUpdateMessage::class.java)
        .let { message ->
            EnhancedLocationUpdate(
                message.location.toLocation(),
                message.predictedLocations.map { it.toLocation() },
                message.type
            )
        }
