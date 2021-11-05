package com.ably.tracking.locationprovider

import android.Manifest
import androidx.annotation.RequiresPermission
import com.ably.tracking.Location
import com.ably.tracking.Resolution

typealias LocationHistoryListener = (LocationHistoryData) -> Unit

interface LocationProvider {
    /**
     * Starts the navigation trip which results in location updates from the location engine.
     */
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    fun startTrip()

    /**
     * Stops the navigation trip and closes the whole [MapboxNavigation].
     */
    fun stopAndClose()

    /**
     * Sets a location observer that gets notified when a new raw or enhanced location is received.
     * If there is already a registered location observer it will be replaced by the [locationUpdatesObserver].
     *
     * @param locationUpdatesObserver The location observer to register.
     */
    fun registerLocationObserver(locationUpdatesObserver: LocationUpdatesObserver)

    /**
     * Removes a location observer if it was previously set with [registerLocationObserver].
     */
    fun unregisterLocationObserver()

    /**
     * Changes the [resolution] of the location engine if it's a subtype of the [ResolutionLocationEngine].
     *
     * @param resolution The new resolution to set.
     */
    fun changeResolution(resolution: Resolution)

    /**
     * Removes the currently active route.
     */
    fun clearRoute()

    /**
     * Sets a route with the provided parameters. The route starts in [currentLocation] and ends in [destination].
     * When the route is successfully set then it calls the [routeDurationCallback] with the estimated route duration.
     *
     * @param currentLocation The current location of the [Publisher].
     * @param destination The destination of the [Trackable].
     * @param routingProfile The routing profile for the route.
     * @param routeDurationCallback The function that's called with the ETA of the route in milliseconds. If something goes wrong it will be called with [MapException].
     */
    fun setRoute(
        currentLocation: Location,
        destination: Destination,
        routingProfile: RoutingProfile,
        routeDurationCallback: (Result<Long>) -> Unit
    )

    /**
     * Sets a location history listener that will be notified when a trip history is ready.
     *
     * @param listener The function to call when location history data is ready.
     */
    fun setLocationHistoryListener(listener: LocationHistoryListener?)
}
