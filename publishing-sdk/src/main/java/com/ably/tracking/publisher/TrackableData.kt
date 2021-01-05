package com.ably.tracking.publisher

import android.location.Location
import com.ably.tracking.Resolution
import io.ably.lib.realtime.Channel

data class TrackableData(
    val channel: Channel,
    val subscribers: MutableSet<Subscriber> = mutableSetOf(),
    val resolution: Resolution? = null,
    val lastSentRaw: Location? = null,
    val lastSentEnhanced: Location? = null
)
