package com.ably.tracking.common

import com.ably.tracking.Location
import com.google.gson.Gson
import io.ably.lib.types.Message


fun Location.toMessage(): LocationMessage =
    LocationMessage(
        GeoJsonTypes.FEATURE,
        LocationGeometry(GeoJsonTypes.POINT, listOf(longitude, latitude, altitude)),
        LocationProperties(
            accuracy,
            bearing,
            speed,
            time.toDouble() / MILLISECONDS_PER_SECOND
        )
    )

fun LocationMessage.toTracking(): Location =
    Location(
        longitude = geometry.coordinates[GEOMETRY_LONG_INDEX],
        latitude = geometry.coordinates[GEOMETRY_LAT_INDEX],
        altitude = geometry.coordinates[GEOMETRY_ALT_INDEX],
        accuracy = properties.accuracyHorizontal,
        bearing = properties.bearing,
        speed = properties.speed,
        time = (properties.time * MILLISECONDS_PER_SECOND).toLong()
    )

fun Message.getLocationMessages(gson: Gson): List<LocationMessage> =
    gson.fromJson(data as String, Array<LocationMessage>::class.java).toList()
