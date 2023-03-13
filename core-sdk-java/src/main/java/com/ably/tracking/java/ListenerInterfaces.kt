package com.ably.tracking.java

import com.ably.tracking.LocationUpdate
import com.ably.tracking.Resolution
import com.ably.tracking.TrackableState
import com.ably.tracking.common.ConnectionState

/**
 * Defines an interface, to be implemented in Java code utilising the Ably Asset Tracking SDKs, allowing that code to
 * handle events containing [LocationUpdate] information.
 */
interface LocationUpdateListener {
    fun onLocationUpdate(locationUpdate: LocationUpdate)
}

/**
 * Defines an interface, to be implemented in Java code utilising the Ably Asset Tracking SDKs, allowing that code to
 * handle events indicating the state of a trackable.
 */
interface TrackableStateListener {
    fun onStateChanged(trackableState: TrackableState)
}

/**
 * Defines an interface, to be implemented in Java code utilising the Ably Asset Tracking SDKs, allowing that code to
 * handle events containing the ably connection state changes.
 */
interface ConnectionStateListener {
    fun onAblyConnectionStateChanged(connectionState: ConnectionState)
}

/**
 * Defines an interface, to be implemented in Java code utilising the Ably Asset Tracking SDKs, allowing that code to
 * handle events containing the publisher presence changes.
 */
interface PublisherPresenceListener {
    fun onPublisherPresenceChanged(isPresent: Boolean)
}

/**
 * Defines an interface, to be implemented in Java code utilising the Ably Asset Tracking SDKs, allowing that code to
 * handle events containing the resolution changes.
 */
interface ResolutionListener {
    fun onResolutionChanged(resolution: Resolution)
}

/**
 * Defines an interface, to be implemented in Java code utilising the Ably Asset Tracking SDKs, allowing that code to
 * handle events containing the location update interval changes.
 */
interface LocationUpdateIntervalListener {
    fun onLocationUpdateIntervalChanged(locationUpdateIntervalInMilliseconds: Long)
}
