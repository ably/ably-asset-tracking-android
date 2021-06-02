package com.ably.tracking.common

import com.ably.tracking.Location

data class TripMetadata(
    val trackingId: String,
    val timestamp: Long,
    val originLocation: Location,
    val destinationLocation: Location?
)
