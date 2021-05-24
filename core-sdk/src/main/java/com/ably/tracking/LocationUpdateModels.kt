package com.ably.tracking

open class LocationUpdate(val location: Location, val skippedLocations: List<Location>) {
    override fun equals(other: Any?): Boolean =
        when (other) {
            is LocationUpdate -> location == other.location && skippedLocations == other.skippedLocations
            else -> false
        }
}

class EnhancedLocationUpdate(
    location: Location,
    skippedLocations: List<Location>,
    val intermediateLocations: List<Location>,
    val type: LocationUpdateType
) : LocationUpdate(location, skippedLocations) {
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

data class Location(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val accuracy: Float,
    val bearing: Float,
    val speed: Float,
    val time: Long
) {
    fun toAndroid(): android.location.Location {
        return android.location.Location("fused").apply {
            longitude = this@Location.longitude
            latitude = this@Location.latitude
            altitude = this@Location.altitude
            accuracy = this@Location.accuracy
            bearing = this@Location.bearing
            speed = this@Location.speed
            time = this@Location.time
        }
    }
}
