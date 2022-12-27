package com.ably.tracking

/**
 * Represents a raw location update of the publisher.
 */
open class LocationUpdate(
    /**
     * Current location of the publisher.
     */
    val location: Location,

    /**
     * List of publisher locations that were skipped since the last sent location update.
     * A location can be skipped due to the active [Resolution] or network issues.
     * This list may be empty.
     */
    val skippedLocations: List<Location>,
) {
    override fun equals(other: Any?): Boolean =
        when {
            other is LocationUpdate -> location == other.location && skippedLocations == other.skippedLocations
            else -> false
        }

    override fun hashCode(): Int {
        var result = location.hashCode()
        result = 31 * result + skippedLocations.hashCode()
        return result
    }
}

/**
 * Represents an enhanced location update of the publisher.
 * Enhanced locations can be predicted from previous locations (e.g. when the GPS signal is lost)
 * or just enhanced (e.g. snapped to the closest road).
 */
class EnhancedLocationUpdate(
    location: Location,
    skippedLocations: List<Location>,

    /**
     * List of predicted location points leading up to the [location] of this enhanced location update.
     * This list may be empty.
     */
    val intermediateLocations: List<Location>,

    /**
     * The type of [location] of this enhanced location update.
     */
    val type: LocationUpdateType
) : LocationUpdate(location, skippedLocations) {
    override fun equals(other: Any?): Boolean =
        when {
            !super.equals(other) -> false
            other is EnhancedLocationUpdate -> intermediateLocations == other.intermediateLocations && type == other.type
            else -> false
        }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + intermediateLocations.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }
}

/**
 * Represents the type of an enhanced location update.
 */
enum class LocationUpdateType {
    /**
     * The location was predicted using the previous locations of the publisher.
     */
    @Deprecated("Predictions are disabled internally so we will never receive a predicted location")
    PREDICTED,

    /**
     * The location is enhanced but not predicted.
     */
    ACTUAL,
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
    init {
        require(latitude.isFinite()) {
            "latitude must be finite, got '$latitude'"
        }
        require(longitude.isFinite()) {
            "longitude must be finite, got '$longitude'"
        }
        require(altitude.isFinite()) {
            "altitude must be finite, got '$altitude'"
        }
    }

    /**
     * Utility function that returns a new, sanitized Location with no non-finite members.
     * A sanitized location contains no NaNs and so can be safely serialised into JSON without throwing an exception.
     */
    fun sanitize(): Location {
        return Location(
            latitude,
            longitude,
            altitude,
            if (accuracy.isFinite()) accuracy else 0.0f,
            if (bearing.isFinite()) bearing else 0.0f,
            if (speed.isFinite()) speed else 0.0f,
            time,
        )
    }

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
