package com.ably.tracking

import android.location.Location

interface LocationListener {
    fun onLocationUpdated(location: Location)
}

typealias Handler<T> = (T) -> Unit
typealias LocationHandler = Handler<Location>

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
) {
    /**
     * Creates an ErrorInformation instance representing an error generated internally from within the Ably Asset
     * Tracking SDK.
     */
    constructor(message: String) : this(10001, 0, message, null, null)
}

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
typealias ConnectionStateChangeHandler = Handler<ConnectionStateChange>

typealias LocationHistoryHandler = Handler<String>

sealed class Result<T> {
    val isSuccess: Boolean get() = this is SuccessResult<*>
}

data class SuccessResult<T>(val result: T) : Result<T>()
data class FailureResult<T>(val errorInformation: ErrorInformation) : Result<T>()

interface ResultListener<T> {
    fun onResult(result: Result<T>)
}

typealias ResultHandler<T> = Handler<Result<T>>

interface AssetStatusListener {
    fun onStatusChanged(isOnline: Boolean)
}

typealias AssetStatusHandler = Handler<Boolean>
