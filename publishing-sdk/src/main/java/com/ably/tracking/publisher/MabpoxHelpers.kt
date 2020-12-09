package com.ably.tracking.publisher

import android.location.Location
import com.mapbox.geojson.Point

/**
 *  Returns list of [Point] that Mapbox will treat as a route for a trip.
 *  First element in the list is the starting position.
 *  Last element in the list is the final position.
 * */
internal fun getRouteCoordinates(
    startingLocation: Location,
    destination: Destination
): List<Point> =
    listOf(
        Point.fromLngLat(startingLocation.longitude, startingLocation.latitude),
        Point.fromLngLat(destination.longitude, destination.latitude)
    )
