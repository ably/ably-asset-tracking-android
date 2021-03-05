package com.ably.tracking.java

import com.ably.tracking.AssetState
import com.ably.tracking.LocationUpdate

/**
 * Defines an interface, to be implemented in Java code utilising the Ably Asset Tracking SDKs, allowing that code to
 * handle events containing [LocationUpdate] information.
 */
interface LocationUpdateListener {
    fun onLocationUpdate(locationUpdate: LocationUpdate)
}

/**
 * Defines an interface, to be implemented in Java code utilising the Ably Asset Tracking SDKs, allowing that code to
 * handle events indicating the state of an asset.
 */
interface AssetStateListener {
    fun onStateChanged(assetState: AssetState)
}
