package com.ably.tracking.publisher

import com.ably.tracking.Location
import com.ably.tracking.common.METERS_PER_KILOMETER
import com.mapbox.geojson.Point
import com.mapbox.turf.TurfMeasurement
import kotlin.math.abs

internal fun Location.distanceInMetersFrom(location: Location): Double =
    distanceInMetersFrom(location.latitude, location.longitude)

internal fun Location.distanceInMetersFrom(destination: Destination): Double =
    distanceInMetersFrom(destination.latitude, destination.longitude)

internal fun Location.distanceInMetersFrom(lat: Double, lng: Double): Double =
    TurfMeasurement.distance(Point.fromLngLat(longitude, latitude), Point.fromLngLat(lng, lat)) * METERS_PER_KILOMETER

internal fun Location.timeFrom(location: Location): Long = abs(time - location.time)
