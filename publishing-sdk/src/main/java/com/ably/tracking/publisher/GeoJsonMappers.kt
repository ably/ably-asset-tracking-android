package com.ably.tracking.publisher

import android.location.Location
import com.google.gson.Gson
import io.ably.lib.types.Message

internal fun Location.toGeoJson(): GeoJsonMessage =
    GeoJsonMessage(
        GeoJsonTypes.FEATURE,
        GeoJsonGeometry(GeoJsonTypes.POINT, listOf(longitude, latitude)),
        GeoJsonProperties(
            accuracy,
            altitude,
            bearing,
            speed,
            time.toDouble() / MILLISECONDS_PER_SECOND
        )
    )

internal fun GeoJsonMessage.toJsonArray(gson: Gson): String =
    gson.toJson(listOf(this))

internal fun List<GeoJsonMessage>.toJsonArray(gson: Gson): String =
    gson.toJson(this)

internal fun GeoJsonMessage.toLocation(): Location =
    Location(LOCATION_TYPE_FUSED).apply {
        longitude = geometry.coordinates[GEOMETRY_LONG_INDEX]
        latitude = geometry.coordinates[GEOMETRY_LAT_INDEX]
        accuracy = properties.accuracyHorizontal
        altitude = properties.altitude
        bearing = properties.bearing
        speed = properties.speed
        time = (properties.time * MILLISECONDS_PER_SECOND).toLong()
    }

internal fun Message.getGeoJsonMessages(gson: Gson): List<GeoJsonMessage> =
    gson.fromJson(data as String, Array<GeoJsonMessage>::class.java).toList()
