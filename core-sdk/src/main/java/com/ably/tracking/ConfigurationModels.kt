package com.ably.tracking

data class AblyConfiguration(val apiKey: String, val clientId: String)

data class LogConfiguration(val enabled: Boolean) // TODO - specify config

enum class Priority {
    NO_POWER,
    LOW_POWER,
    BALANCED_POWER_ACCURACY,
    HIGH_ACCURACY,
}

/**
 * Governs how often to sample locations, at what level of positional accuracy, and how often to send them to
 * subscribers.
 */
data class Resolution(
    // our own enum for this but should map thru logically to Android SDK base offering
    // contention: most aggressive of all these wins
    /**
     * ssss
     */
    val priority: Priority,

    /**
     * Maximum time between updates, in milliseconds.
     *
     * Location updates whose timestamp differs from the last captured update timestamp by less that this value are to
     * be filtered out.
     *
     * Used to govern the frequency of updates requested from the underlying location provider, as well as the frequency
     * of messages broadcast to subscribers.
     */
    val delay: Long,

    /**
     * Minimum positional granularity required, in metres.
     *
     * Location updates whose position differs from the last known position by a distance smaller than this value are to
     * be filtered out.
     *
     * Used to configure the underlying loation provider, as well as to filter the broadcast of updates to subsribers.
     */
    val displacement: Double
)
