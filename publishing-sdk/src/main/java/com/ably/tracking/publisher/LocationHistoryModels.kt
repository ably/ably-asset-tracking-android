package com.ably.tracking.publisher

import com.ably.tracking.GeoJsonGeometry
import com.ably.tracking.GeoJsonMessage
import com.ably.tracking.GeoJsonProperties
import com.ably.tracking.common.GEOMETRY_ALT_INDEX
import com.ably.tracking.common.GEOMETRY_LAT_INDEX
import com.ably.tracking.common.GEOMETRY_LONG_INDEX
import com.ably.tracking.common.GeoJsonTypes
import com.mapbox.navigation.core.replay.history.ReplayEventBase
import com.mapbox.navigation.core.replay.history.ReplayEventLocation
import com.mapbox.navigation.core.replay.history.ReplayEventUpdateLocation

val LOCATION_HISTORY_VERSION = 1

data class LocationHistoryData(
    val events: List<GeoJsonMessage>
) {
    val version: Int = LOCATION_HISTORY_VERSION
}

fun List<ReplayEventBase>.toGeoJsonMessages(): List<GeoJsonMessage> =
    filterIsInstance<ReplayEventUpdateLocation>()
        .map { event ->
            GeoJsonMessage(
                GeoJsonTypes.FEATURE,
                GeoJsonGeometry(
                    GeoJsonTypes.POINT, listOf(event.location.lon, event.location.lat, event.location.altitude ?: 0.0)
                ),
                GeoJsonProperties(
                    // TODO - check if those default values are OK
                    // https://github.com/ably/ably-asset-tracking-android/issues/191
                    event.location.accuracyHorizontal?.toFloat() ?: 0f,
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
                geoJson.geometry.coordinates[GEOMETRY_ALT_INDEX],
                geoJson.properties.accuracyHorizontal.toDouble(),
                geoJson.properties.bearing.toDouble(),
                geoJson.properties.speed.toDouble()
            )
        )
    }
