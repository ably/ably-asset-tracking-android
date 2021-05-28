package com.ably.tracking.common

import com.ably.tracking.GeoJsonGeometry
import com.ably.tracking.GeoJsonMessage
import com.ably.tracking.GeoJsonProperties
import com.ably.tracking.Location
import com.google.gson.Gson
import io.ably.lib.types.Message

fun Location.toGeoJson(): GeoJsonMessage =
    GeoJsonMessage(
        GeoJsonTypes.FEATURE,
        GeoJsonGeometry(GeoJsonTypes.POINT, listOf(longitude, latitude, altitude)),
        GeoJsonProperties(
            accuracy,
            bearing,
            speed,
            time.toDouble() / MILLISECONDS_PER_SECOND
        )
    )

fun GeoJsonMessage.toJsonArray(gson: Gson): String =
    gson.toJson(listOf(this))

fun List<GeoJsonMessage>.toJsonArray(gson: Gson): String =
    gson.toJson(this)

fun GeoJsonMessage.toLocation(): Location =
    Location(
        longitude = geometry.coordinates[GEOMETRY_LONG_INDEX],
        latitude = geometry.coordinates[GEOMETRY_LAT_INDEX],
        altitude = geometry.coordinates[GEOMETRY_ALT_INDEX],
        accuracy = properties.accuracyHorizontal,
        bearing = properties.bearing,
        speed = properties.speed,
        time = (properties.time * MILLISECONDS_PER_SECOND).toLong()
    )

fun Message.getGeoJsonMessages(gson: Gson): List<GeoJsonMessage> =
    gson.fromJson(data as String, Array<GeoJsonMessage>::class.java).toList()
