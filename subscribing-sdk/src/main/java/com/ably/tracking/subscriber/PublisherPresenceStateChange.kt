package com.ably.tracking.subscriber

import com.ably.tracking.ErrorInformation

/**
 * This represents the "overall" publisher presence - as long as there is
 * at least one individual publisher present, it will be [PRESENT].
 *
 * [UNKNOWN] will be reserved for occasions when it is not possible to be sure
 * if a publisher is currently present, for example if the subscriber is not
 * connected to Ably.
 */
enum class PublisherPresenceState {
    PRESENT,
    ABSENT,
    UNKNOWN,
}

/**
 * For an individual publisher, this enum represents their last known state.
 */
enum class LastKnownPublisherState {
    PRESENT,
    ABSENT,
}

/**
 * Represents a publisher that AAT knows (via presence history of presence events)
 * has been present at some point. This class can be used to identify previous publishers
 * and when they might have been present.
 */
data class KnownPublisher(
    val clientId: String,
    val connectionId: String,
    val state: LastKnownPublisherState,
    val lastSeen: Long
)

data class PublisherPresenceStateChange(
    /**
     * The current overall publisher presence state.
     */
    val current: PublisherPresenceState,

    /**
     * The previous overall publisher presence state.
     */
    val previous: PublisherPresenceState,

    /**
     * Any error information associated with the state change (e.g. the connection went offline)
     */
    val reason: ErrorInformation?,

    /**
     * The timestamp associated with this state change. In the event of the connection going offline, this
     * timestamp will be the time at which the connection went offline.
     *
     * Where the state change was a result of a presence message, this timestamp will be the timestamp
     * associated with that presence message.
     */
    val timestamp: Long,

    /**
     * Any given trackable may have multiple publishers associated. This list provides
     * the last known states of each individual publisher.
     */
    val publishers: List<KnownPublisher>,
)
