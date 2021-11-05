package com.ably.tracking.publisher

import com.ably.tracking.Location
import com.ably.tracking.locationprovider.Destination

internal class ThresholdChecker {
    fun isThresholdReached(
        threshold: Proximity,
        currentLocation: Location,
        currentTimeInMilliseconds: Long,
        destination: Destination?,
        estimatedArrivalTimeInMilliseconds: Long?
    ): Boolean =
        when (threshold) {
            is DefaultProximity -> isDefaultThresholdReached(
                threshold,
                currentLocation,
                currentTimeInMilliseconds,
                destination,
                estimatedArrivalTimeInMilliseconds
            )
        }

    private fun isDefaultThresholdReached(
        threshold: DefaultProximity,
        currentLocation: Location,
        currentTimeInMilliseconds: Long,
        destination: Destination?,
        estimatedArrivalTimeInMilliseconds: Long?
    ): Boolean {
        val spatialProximityReached = isSpatialProximityReached(threshold, currentLocation, destination)
        val temporalProximityReached =
            isTemporalProximityReached(threshold, currentTimeInMilliseconds, estimatedArrivalTimeInMilliseconds)
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
        currentTimeInMilliseconds: Long,
        estimatedArrivalTimeInMilliseconds: Long?
    ): Boolean =
        if (threshold.temporal != null && estimatedArrivalTimeInMilliseconds != null) {
            estimatedArrivalTimeInMilliseconds - currentTimeInMilliseconds < threshold.temporal
        } else {
            false
        }
}
