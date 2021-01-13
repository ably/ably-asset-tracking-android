package com.ably.tracking

import android.location.Location

/**
 * Defines an interface, to be implemented in Java code utilising the Ably Asset Tracking SDKs, allowing that code to
 * handle events containing [Location] information.
 */
interface LocationListener {
    fun onLocationUpdated(location: Location)
}

/**
 * Defines a function type, to be implemented in Kotlin code utilising the Ably Asset Tracking SDKs, allowing that code
 * to handle events using that function as a callback (typically using a lamda expression).
 */
typealias Handler<T> = (T) -> Unit

/**
 * Defines a function type, to be implemented in Kotlin code utilising the Ably Asset Tracking SDKs, allowing that code
 * to handle events containing [Location] information.
 */
typealias LocationHandler = Handler<Location>

/**
 * The state of connectivity to the Ably service.
 */
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
    constructor(message: String) : this(100000, 0, message, null, null)
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

/**
 * Defines a function type, to be implemented in Kotlin code utilising the Ably Asset Tracking SDKs, allowing that code
 * to handle events containing location history information in [String] format.
 */
typealias LocationHistoryHandler = Handler<String>

/**
 * The result of an asynchronous operation, which will either have been successful or will have failed.
 *
 * Methods returning instances of this class to callback implementations, where there is no explicit type `T` to be
 * returned, use a different signature depending on the method overload:
 *
 * - for Kotlin code: `Result<Unit>`
 * - for Java code: `Result<Void?>`
 */
sealed class Result<T> {
    /**
     * `true` if the operation was successful, meaning that this instance is of type [SuccessResult].
     */
    val isSuccess: Boolean get() = this is SuccessResult<*>
}

/**
 * Conveys that an asynchronous operation completed successfully.
 */
data class SuccessResult<T>(
    /**
     * The product of the operation, if applicable.
     */
    val result: T
) : Result<T>()

/**
 * Conveys that an asynchronous operation failed.
 */
data class FailureResult<T>(
    /**
     * The reason why the operation failed.
     */
    val errorInformation: ErrorInformation
) : Result<T>()

/**
 * Defines an interface, to be implemented in Java code utilising the Ably Asset Tracking SDKs, allowing that code to
 * handle an event indicating the result of an asynchronous operation.
 */
interface ResultListener<T> {
    fun onResult(result: Result<T>)
}

/**
 * Defines a function type, to be implemented in Kotlin code utilising the Ably Asset Tracking SDKs, allowing that code
 * to handle an event indicating the result of an asynchronous operation.
 */
typealias ResultHandler<T> = Handler<Result<T>>

interface AssetStatusListener {
    fun onStatusChanged(isOnline: Boolean)
}

typealias AssetStatusHandler = Handler<Boolean>
