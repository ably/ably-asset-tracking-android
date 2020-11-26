package com.ably.tracking.subscriber

import android.location.Location
import com.google.gson.Gson
import io.ably.lib.types.Message

fun GeoJsonMessage.toLocation(): Location =
    Location(LOCATION_TYPE_FUSED).apply {
        longitude = geometry.coordinates[GEOMETRY_LONG_INDEX]
        latitude = geometry.coordinates[GEOMETRY_LAT_INDEX]
        accuracy = properties.accuracyHorizontal
        altitude = properties.altitude
        bearing = properties.bearing
        speed = properties.speed
        time = (properties.time * MILLISECONDS_PER_SECOND).toLong()
    }

fun Message.getGeoJsonMessages(gson: Gson): List<GeoJsonMessage> =
    gson.fromJson(data as String, Array<GeoJsonMessage>::class.java).toList()
