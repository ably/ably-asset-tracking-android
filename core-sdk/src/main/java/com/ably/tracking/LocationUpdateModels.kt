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

/**
 * Represents the geographic location of the publisher.
 */
data class Location(
    /**
     * Latitude of the location in degrees.
     */
    val latitude: Double,
    /**
     * Longitude of the location in degrees.
     */
    val longitude: Double,
    /**
     * Altitude of the location in meters above the WGS 84 reference ellipsoid.
     * 0.0 if not available.
     */
    val altitude: Double,
    /**
     * Estimated horizontal accuracy of the location, radial, in meters.
     * 0.0 if not available.
     */
    val accuracy: Float,
    /**
     * Bearing of the location in degrees.
     * 0.0 if not available.
     */
    val bearing: Float,
    /**
     * Speed of the location in meters per second.
     * 0.0 if not available.
     */
    val speed: Float,
    /**
     * Timestamp of the location in milliseconds since the epoch.
     */
    val time: Long
) {
    /**
     * Convenience function that maps the [Location] object to Android's [android.location.Location] object.
     */
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
