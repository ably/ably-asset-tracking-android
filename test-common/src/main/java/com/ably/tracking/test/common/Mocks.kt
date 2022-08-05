package com.ably.tracking.test.common

import com.ably.tracking.Location
import com.ably.tracking.common.message.GeoJsonTypes
import com.ably.tracking.common.message.LocationGeometry
import com.ably.tracking.common.message.LocationMessage
import com.ably.tracking.common.message.LocationProperties

fun createLocation(lat: Double = 0.0, lng: Double = 0.0, timestamp: Long = 0): Location =
    Location(lat, lng, 0.0, 0f, 0f, 0f, timestamp)

fun anyLocation() = createLocation()

fun anyLocationMessage(): LocationMessage =
    LocationMessage(
        GeoJsonTypes.FEATURE,
        LocationGeometry(GeoJsonTypes.POINT, listOf(1.0, 2.0, 3.0)),
        LocationProperties(1f, 2f, 3f, 4.0)
    )
