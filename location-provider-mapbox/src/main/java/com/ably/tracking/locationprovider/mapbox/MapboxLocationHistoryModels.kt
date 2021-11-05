package com.ably.tracking.locationprovider.mapbox

import com.ably.tracking.Location
import com.ably.tracking.common.MILLISECONDS_PER_SECOND
import com.ably.tracking.locationprovider.LocationHistoryData
import com.google.gson.annotations.SerializedName
import com.mapbox.navigation.core.replay.history.ReplayEventBase
import com.mapbox.navigation.core.replay.history.ReplayEventLocation
import com.mapbox.navigation.core.replay.history.ReplayEventUpdateLocation

val LOCATION_HISTORY_VERSION = 1

data class MapboxLocationHistoryData(
    @SerializedName("events") override val events: List<Location>
) : LocationHistoryData {
    @SerializedName("version")
    override val version: Int = LOCATION_HISTORY_VERSION
}

fun List<ReplayEventBase>.toLocations(): List<Location> =
    filterIsInstance<ReplayEventUpdateLocation>()
        .map { event ->
            Location(
                event.location.lon,
                event.location.lat,
                event.location.altitude ?: 0.0,
                // TODO - check if those default values are OK
                // https://github.com/ably/ably-asset-tracking-android/issues/191
                event.location.accuracyHorizontal?.toFloat() ?: 0f,
                event.location.bearing?.toFloat() ?: 0f,
                event.location.speed?.toFloat() ?: 0f,
                (event.eventTimestamp * MILLISECONDS_PER_SECOND).toLong()
            )
        }

fun List<Location>.toReplayEvents(): List<ReplayEventUpdateLocation> =
    map { location ->
        ReplayEventUpdateLocation(
            location.time / MILLISECONDS_PER_SECOND.toDouble(),
            ReplayEventLocation(
                location.longitude,
                location.latitude,
                null,
                null,
                location.altitude,
                location.accuracy.toDouble(),
                location.bearing.toDouble(),
                location.speed.toDouble()
            )
        )
    }
