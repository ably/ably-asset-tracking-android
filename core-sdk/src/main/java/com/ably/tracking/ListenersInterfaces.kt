package com.ably.tracking

import android.location.Location
import io.ably.lib.realtime.ConnectionStateListener

interface LocationUpdatedListener {
    fun onLocationUpdated(location: Location)
}

fun asLocationUpdatedListener(operation: (Location) -> Unit): LocationUpdatedListener =
    object : LocationUpdatedListener {
        override fun onLocationUpdated(location: Location) = operation(location)
    }

interface AblyStateChangeListener {
    fun onConnectionStateChange(connectionStateChange: ConnectionStateListener.ConnectionStateChange)
}

fun asAblyStateChangeListener(operation: (ConnectionStateListener.ConnectionStateChange) -> Unit): AblyStateChangeListener =
    object : AblyStateChangeListener {
        override fun onConnectionStateChange(connectionStateChange: ConnectionStateListener.ConnectionStateChange) =
            operation(connectionStateChange)
    }

interface LocationHistoryListener {
    fun onHistoryReady(historyData: String)
}

fun asLocationHistoryListener(operation: (String) -> Unit): LocationHistoryListener =
    object : LocationHistoryListener {
        override fun onHistoryReady(historyData: String) = operation(historyData)
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

fun asResultHandler(operation: (Result) -> Unit): ResultHandler =
    object : ResultHandler {
        override fun onResult(result: Result) = operation(result)
    }

interface AssetStatusListener {
    fun onStatusChanged(isOnline: Boolean)
}

fun asAssetStatusListener(operation: (Boolean) -> Unit): AssetStatusListener =
    object : AssetStatusListener {
        override fun onStatusChanged(isOnline: Boolean) = operation(isOnline)
    }
