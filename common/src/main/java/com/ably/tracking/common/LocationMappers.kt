package com.ably.tracking.common

import com.ably.tracking.Location

fun android.location.Location.toAssetTracking(): Location =
    Location(latitude, longitude, altitude, accuracy, bearing, speed, time)
