package com.ably.tracking.locationprovider

import com.ably.tracking.Location

/**
 * An interface which enables observing location updates.
 */
interface LocationUpdatesObserver {
    /**
     * Called when a new raw location update is ready.
     *
     * @param rawLocation the current raw location.
     */
    fun onRawLocationChanged(rawLocation: Location)

    /**
     * Called when a new enhanced location update is ready.
     *
     * @param enhancedLocation the current enhanced location.
     * @param intermediateLocations a list (can be empty) of predicted location points leading up to the current update.
     */
    fun onEnhancedLocationChanged(enhancedLocation: Location, intermediateLocations: List<Location>)
}
