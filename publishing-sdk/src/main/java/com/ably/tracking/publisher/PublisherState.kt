package com.ably.tracking.publisher

enum class PublisherState {
    /**
     * The publisher is created but has no connection to Ably.
     */
    IDLE,

    /**
     * The publisher is trying to connect to Ably.
     */
    CONNECTING,

    /**
     * The publisher is connected to Ably.
     */
    CONNECTED,

    /**
     * The publisher is trying to disconnect from Ably.
     */
    DISCONNECTING,

    /**
     * The publisher is stopped and should not be used anymore.
     */
    STOPPED
}
