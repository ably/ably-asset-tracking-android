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

/**
 * Information about an error reported by the Ably service.
 */
data class ErrorInformation(
    /**
     * Ably specific error code. Defined [here](https://github.com/ably/ably-common/blob/main/protocol/errors.json).
     */
    val code: Int,

    /**
     * Analogous to HTTP status code.
     */
    val statusCode: Int,

    /**
     * An explanation of what went wrong, in a format readable by humans.
     * Can be written to logs or presented to users, but is not intended to be machine parsed.
     */
    val message: String,

    /**
     * A URL for customers to find more help on the error code.
     */
    val href: String?,

    /**
     * An error underlying this error which caused this failure.
     */
    val cause: ErrorInformation?
)

/**
 * A change in state of a connection to the Ably service.
 */
data class ConnectionStateChange(
    /**
     * The new state, which is now current.
     */
    val state: ConnectionState,

    /**
     * The previous state.
     */
    val previousState: ConnectionState,

    /**
     * Information about what went wrong, if [state] is failed or failing in some way.
     */
    val errorInformation: ErrorInformation?
)

/**
 * Defines the signature of a function accepting state change events relating connectivity to the Ably service.
 */
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
