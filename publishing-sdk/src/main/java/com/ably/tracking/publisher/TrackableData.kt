package com.ably.tracking.publisher

import android.location.Location
import io.ably.lib.realtime.Channel

data class TrackableData(
    val channel: Channel,
    val lastSentRaw: Location? = null,
    val lastSentEnhanced: Location? = null
)
