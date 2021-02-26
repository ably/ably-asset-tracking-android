package com.ably.tracking

import android.location.Location

open class LocationUpdate(val location: Location, val batteryLevel: Float?)

class EnhancedLocationUpdate(
    location: Location,
    batteryLevel: Float?,
    val intermediateLocations: List<Location>,
    val type: LocationUpdateType
) : LocationUpdate(location, batteryLevel)

enum class LocationUpdateType {
    PREDICTED, ACTUAL
}
