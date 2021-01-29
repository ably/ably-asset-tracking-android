package com.ably.tracking

/**
 * Defines a function type, to be implemented in Kotlin code utilising the Ably Asset Tracking SDKs, allowing that code
 * to handle events using that function as a callback (typically using a lamda expression).
 */
typealias Handler<T> = (T) -> Unit

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

// TODO to be removed
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
