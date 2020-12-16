package com.ably.tracking.publisher

import android.location.Location

internal class ThresholdChecker {
    fun isThresholdReached(
        threshold: Proximity,
        currentLocation: Location,
        destination: Destination?,
        estimatedArrivalTimeInMilliseconds: Long?
    ): Boolean =
        when (threshold) {
            is DefaultProximity -> isDefaultThresholdReached(
                threshold,
                currentLocation,
                destination,
                estimatedArrivalTimeInMilliseconds
            )
        }

    private fun isDefaultThresholdReached(
        threshold: DefaultProximity,
        currentLocation: Location,
        destination: Destination?,
        estimatedArrivalTimeInMilliseconds: Long?
    ): Boolean {
        val spatialProximityReached = isSpatialProximityReached(threshold, currentLocation, destination)
        val temporalProximityReached = isTemporalProximityReached(threshold, estimatedArrivalTimeInMilliseconds)
        return spatialProximityReached || temporalProximityReached
    }

    private fun isSpatialProximityReached(
        threshold: DefaultProximity,
        currentLocation: Location,
        destination: Destination?
    ): Boolean =
        if (threshold.spatial != null && destination != null) {
            currentLocation.distanceInMetersFrom(destination) < threshold.spatial
        } else {
            false
        }

    private fun isTemporalProximityReached(
        threshold: DefaultProximity,
        estimatedArrivalTimeInMilliseconds: Long?
    ): Boolean =
        if (threshold.temporal != null && estimatedArrivalTimeInMilliseconds != null) {
            estimatedArrivalTimeInMilliseconds - System.currentTimeMillis() < threshold.temporal
        } else {
            false
        }
}
