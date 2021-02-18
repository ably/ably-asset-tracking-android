package com.ably.tracking.publisher

import com.ably.tracking.GeoJsonGeometry
import com.ably.tracking.GeoJsonMessage
import com.ably.tracking.GeoJsonProperties
import com.ably.tracking.common.GEOMETRY_LAT_INDEX
import com.ably.tracking.common.GEOMETRY_LONG_INDEX
import com.ably.tracking.common.GeoJsonTypes
import com.mapbox.navigation.core.replay.history.ReplayEventBase
import com.mapbox.navigation.core.replay.history.ReplayEventLocation
import com.mapbox.navigation.core.replay.history.ReplayEventUpdateLocation

val LOCATION_HISTORY_VERSION = "1.0"

data class LocationHistoryData(
    val version: String,
    val events: List<GeoJsonMessage>
)

fun List<ReplayEventBase>.toGeoJsonMessages(): List<GeoJsonMessage> =
    filterIsInstance<ReplayEventUpdateLocation>()
        .map { event ->
            GeoJsonMessage(
                GeoJsonTypes.FEATURE,
                GeoJsonGeometry(
                    GeoJsonTypes.POINT, listOf(event.location.lon, event.location.lat)
                ),
                GeoJsonProperties(
                    // TODO - check if those default values are OK
                    event.location.accuracyHorizontal?.toFloat() ?: 0f,
                    event.location.altitude ?: 0.0,
                    event.location.bearing?.toFloat() ?: 0f,
                    event.location.speed?.toFloat() ?: 0f,
                    event.eventTimestamp
                )
            )
        }

fun List<GeoJsonMessage>.toReplayEvents(): List<ReplayEventUpdateLocation> =
    map { geoJson ->
        ReplayEventUpdateLocation(
            geoJson.properties.time,
            ReplayEventLocation(
                geoJson.geometry.coordinates[GEOMETRY_LONG_INDEX],
                geoJson.geometry.coordinates[GEOMETRY_LAT_INDEX],
                null,
                null,
                geoJson.properties.altitude,
                geoJson.properties.accuracyHorizontal.toDouble(),
                geoJson.properties.bearing.toDouble(),
                geoJson.properties.speed.toDouble()
            )
        )
    }
