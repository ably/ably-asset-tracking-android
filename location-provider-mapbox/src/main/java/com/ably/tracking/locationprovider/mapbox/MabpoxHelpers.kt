package com.ably.tracking.locationprovider.mapbox

import com.ably.tracking.Location
import com.ably.tracking.locationprovider.Destination
import com.ably.tracking.locationprovider.RoutingProfile
import com.mapbox.api.directions.v5.DirectionsCriteria
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

internal fun RoutingProfile.toMapboxProfileName() = when (this) {
    RoutingProfile.DRIVING -> DirectionsCriteria.PROFILE_DRIVING
    RoutingProfile.CYCLING -> DirectionsCriteria.PROFILE_CYCLING
    RoutingProfile.WALKING -> DirectionsCriteria.PROFILE_WALKING
    RoutingProfile.DRIVING_TRAFFIC -> DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
}
