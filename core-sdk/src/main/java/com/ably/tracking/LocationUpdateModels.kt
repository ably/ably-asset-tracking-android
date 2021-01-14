package com.ably.tracking

import android.location.Location

open class LocationUpdate(val location: Location)

class EnhancedLocationUpdate(
    location: Location,
    val intermediateLocations: List<Location>,
    val type: LocationUpdateType
) : LocationUpdate(location)

enum class LocationUpdateType {
    PREDICTED, ACTUAL
}
