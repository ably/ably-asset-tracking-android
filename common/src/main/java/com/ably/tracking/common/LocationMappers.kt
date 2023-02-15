package com.ably.tracking.common

import com.ably.tracking.Location

fun android.location.Location.toAssetTracking(timestamp: Long = time): Result<Location> {
    val accuracy = if (hasAccuracy()) {
        accuracy
    } else {
        Float.NaN
    }

    val bearing = if (hasBearing()) {
        bearing
    } else {
        Float.NaN
    }

    val speed = if (hasSpeed()) {
        speed
    } else {
        Float.NaN
    }

    return Location(latitude, longitude, altitude, accuracy, bearing, speed, timestamp)
        .sanitize()
        .validate()
}
