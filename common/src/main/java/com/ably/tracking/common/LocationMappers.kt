package com.ably.tracking.common

import com.ably.tracking.Location

fun android.location.Location.toAssetTracking(timestamp: Long = time): Location =
    Location(latitude, longitude, altitude, accuracy, bearing, speed, timestamp)
