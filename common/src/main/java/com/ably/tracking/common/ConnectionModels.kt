package com.ably.tracking.common

import com.ably.tracking.ErrorInformation

/**
 * The state of connectivity to the Ably service.
 */
enum class ConnectionState {
    ONLINE, OFFLINE, FAILED,
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
     * Information about what went wrong, if [state] is failed or failing in some way.
     */
    val errorInformation: ErrorInformation?
)
