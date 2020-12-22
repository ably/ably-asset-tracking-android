package com.ably.tracking

import android.location.Location
import io.ably.lib.realtime.ConnectionStateListener

interface LocationUpdatedListener {
    fun onLocationUpdated(location: Location)
}

interface AblyStateChangeListener {
    fun onConnectionStateChange(connectionStateChange: ConnectionStateListener.ConnectionStateChange)
}

interface LocationHistoryListener {
    fun onHistoryReady(historyData: String)
}

interface CallbackHandler {
    fun onSuccess()
    fun onError(exception: Exception)
}

interface RemoveTrackableListener {
    /**
     * Called when the trackable is successfully removed
     *
     * @param wasPresent is true when the object was known to this publisher, being that it was in the tracked set.
     */
    fun onSuccess(wasPresent: Boolean)

    /**
     * Called when an error occurs
     */
    fun onError(exception: Exception)
}

interface AssetStatusListener {
    fun onStatusChanged(isOnline: Boolean)
}
