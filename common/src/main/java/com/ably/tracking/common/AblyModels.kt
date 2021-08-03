package com.ably.tracking.common

import com.ably.tracking.Resolution

data class PresenceMessage(val action: PresenceAction, val data: PresenceData, val clientId: String)

enum class PresenceAction {
    PRESENT_OR_ENTER, LEAVE_OR_ABSENT, UPDATE;
}

data class PresenceData(val type: String, val resolution: Resolution? = null)
