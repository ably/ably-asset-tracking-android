package com.ably.tracking.test.common

import com.ably.tracking.Location

fun createLocation(lat: Double = 0.0, lng: Double = 0.0, timestamp: Long = 0): Location =
    Location(lat, lng, 0.0, 0f, 0f, 0f, timestamp)

fun anyLocation() = createLocation()
