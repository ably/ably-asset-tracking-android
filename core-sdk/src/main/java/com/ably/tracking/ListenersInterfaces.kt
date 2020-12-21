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

interface TrackTrackableListener {
    /**
     * Called when the trackable is successfully added and made the actively tracked object
     */
    fun onSuccess()

    /**
     * Called when an error occurs
     */
    fun onError(exception: Exception)
}

interface AddTrackableListener {
    /**
     * Called when the trackable is successfully added
     */
    fun onSuccess()

    /**
     * Called when an error occurs
     */
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
