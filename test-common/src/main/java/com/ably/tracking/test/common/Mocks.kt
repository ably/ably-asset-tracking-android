package com.ably.tracking.test.common

import android.location.Location
import io.mockk.every
import io.mockk.mockk

fun createLocation(lat: Double = 0.0, lng: Double = 0.0, timestamp: Long = 0): Location =
    mockk<Location>().apply {
        every { time } returns timestamp
        every { longitude } returns lng
        every { latitude } returns lat
    }

fun anyLocation() = createLocation()
