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

interface AssetStatusListener {
    fun onStatusChanged(isOnline: Boolean)
}
