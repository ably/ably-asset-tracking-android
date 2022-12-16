package com.ably.tracking.common

import com.ably.tracking.Resolution

data class PresenceMessage(
    val action: PresenceAction,
    val data: PresenceData,

    /**
     * Combination of Ably `clientId` and `connectionId`.
     * See: https://sdk.ably.com/builds/ably/specification/main/features/#TP3h
     */
    val memberKey: String,
)

enum class PresenceAction {
    PRESENT_OR_ENTER, LEAVE_OR_ABSENT, UPDATE;
}

data class PresenceData(val type: String, val resolution: Resolution? = null, val rawLocations: Boolean? = null)
