package com.ably.tracking

import android.location.Location

interface LocationUpdatedListener {
    fun onLocationUpdated(location: Location)
}

fun asLocationUpdatedListener(operation: (Location) -> Unit): LocationUpdatedListener =
    object : LocationUpdatedListener {
        override fun onLocationUpdated(location: Location) = operation(location)
    }

enum class ConnectionState {
    INITIALIZED,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    SUSPENDED,
    CLOSING,
    CLOSED,
    FAILED,
}

data class ConnectionStateChange(
    val previousState: ConnectionState,
    val state: ConnectionState
)

interface ConnectionStateChangeListener {
    fun onConnectionStateChange(change: ConnectionStateChange)
}

/**
 * Convenience method, adapting the Java-friendly [ConnectionStateChangeListener] API to make it easier and more
 * idiomatic to use from Kotlin code.
 */
fun asConnectionStateChangeListener(operation: (ConnectionStateChange) -> Unit): ConnectionStateChangeListener =
    object : ConnectionStateChangeListener {
        override fun onConnectionStateChange(change: ConnectionStateChange) = operation(change)
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
