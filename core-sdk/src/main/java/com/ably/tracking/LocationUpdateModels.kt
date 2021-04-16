package com.ably.tracking

import android.location.Location

open class LocationUpdate(val location: Location, val batteryLevel: Float?, val skippedLocations: List<Location>) {
    override fun equals(other: Any?): Boolean =
        when (other) {
            is LocationUpdate -> location == other.location && batteryLevel == other.batteryLevel && skippedLocations == other.skippedLocations
            else -> false
        }
}

class EnhancedLocationUpdate(
    location: Location,
    batteryLevel: Float?,
    skippedLocations: List<Location>,
    val intermediateLocations: List<Location>,
    val type: LocationUpdateType
) : LocationUpdate(location, batteryLevel, skippedLocations) {
    override fun equals(other: Any?): Boolean =
        when (other) {
            !super.equals(other) -> false
            is EnhancedLocationUpdate -> intermediateLocations == other.intermediateLocations && type == other.type
            else -> false
        }
}

enum class LocationUpdateType {
    PREDICTED, ACTUAL
}
