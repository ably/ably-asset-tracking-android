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

sealed class Result {
    fun isSuccess(): Boolean = this is SuccessResult
    fun exception(): Exception? = (this as? FailureResult)?.exception
}

class SuccessResult : Result()
data class FailureResult(val exception: Exception) : Result()

interface ResultHandler {
    fun onResult(result: Result)
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
