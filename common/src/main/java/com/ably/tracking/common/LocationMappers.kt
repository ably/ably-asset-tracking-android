package com.ably.tracking.common

import com.ably.tracking.Location

fun android.location.Location.toAssetTracking(timestamp: Long = time): Result<Location> {
    val altitude = if (hasAltitude()) {
        altitude
    } else {
        Double.NaN
    }

    val accuracy = if (hasAccuracy()) {
        accuracy
    } else {
        Location.INVALID_VALUE
    }

    val bearing = if (hasBearing()) {
        bearing
    } else {
        Location.INVALID_VALUE
    }

    val speed = if (hasSpeed()) {
        speed
    } else {
        Location.INVALID_VALUE
    }

    return Location(latitude, longitude, altitude, accuracy, bearing, speed, timestamp)
        .sanitize()
        .validate()
}
