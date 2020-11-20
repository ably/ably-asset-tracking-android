package com.ably.tracking.publisher

import android.location.Location
import com.google.gson.Gson

fun Location.toGeoJson(): GeoJsonMessage =
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

fun GeoJsonMessage.toJsonArray(gson: Gson): String =
    gson.toJson(listOf(this))

fun List<GeoJsonMessage>.toJsonArray(gson: Gson): String =
    gson.toJson(this)
