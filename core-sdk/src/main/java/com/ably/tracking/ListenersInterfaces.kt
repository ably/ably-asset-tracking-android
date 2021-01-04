package com.ably.tracking

import android.location.Location

interface LocationListener {
    fun onLocationUpdated(location: Location)
}

typealias LocationHandler = (Location) -> Unit

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

typealias ConnectionStateChangeHandler = (ConnectionStateChange) -> Unit

typealias LocationHistoryHandler = (String) -> Unit

sealed class Result {
    fun isSuccess(): Boolean = this is SuccessResult
    fun exception(): Exception? = (this as? FailureResult)?.exception
}

class SuccessResult : Result()
data class FailureResult(val exception: Exception) : Result()

interface ResultListener {
    fun onResult(result: Result)
}

typealias ResultHandler = (Result) -> Unit

interface AssetStatusListener {
    fun onStatusChanged(isOnline: Boolean)
}

typealias AssetStatusHandler = (Boolean) -> Unit
