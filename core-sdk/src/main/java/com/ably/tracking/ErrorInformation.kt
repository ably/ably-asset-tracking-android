package com.ably.tracking

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
