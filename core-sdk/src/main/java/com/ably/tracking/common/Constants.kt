package com.ably.tracking.common

const val MILLISECONDS_PER_SECOND = 1000

const val MILLISECONDS_PER_MINUTE = MILLISECONDS_PER_SECOND * 60

const val METERS_PER_KILOMETER = 1000

const val LOCATION_TYPE_FUSED = "fused"

object EventNames {
    const val RAW = "raw"
    const val ENHANCED = "enhanced"
}

object ClientTypes {
    const val SUBSCRIBER = "subscriber"
    const val PUBLISHER = "publisher"
}
