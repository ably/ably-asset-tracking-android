package com.ably.tracking.test.common

import android.location.Location
import io.mockk.every
import io.mockk.mockk

fun createLocation(lat: Double, lng: Double): Location = mockk<Location>().apply {
    every { longitude } returns lng
    every { latitude } returns lat
}

fun anyLocation() = createLocation(1.0, 1.0)
