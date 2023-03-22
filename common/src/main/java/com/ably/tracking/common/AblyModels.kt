package com.ably.tracking.common

import com.ably.tracking.Resolution

/**
 * Encapsulates the properties of an Ably presence message which are needed by asset tracking SDKs.
 */
data class PresenceMessage(
    val action: PresenceAction,
    val data: PresenceData,

    val timestamp: Long,

    /**
     * Combination of Ably `clientId` and `connectionId`.
     * See: https://sdk.ably.com/builds/ably/specification/main/features/#TP3h
     */
    val memberKey: String,
    val clientId: String,
    val connectionId: String,

    val id: String
)

enum class PresenceAction {
    PRESENT_OR_ENTER, LEAVE_OR_ABSENT, UPDATE;
}

data class PresenceData(val type: String, val resolution: Resolution? = null, val rawLocations: Boolean? = null)
