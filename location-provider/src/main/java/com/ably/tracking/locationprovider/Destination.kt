package com.ably.tracking.locationprovider

import com.ably.tracking.Location

data class Destination(
    val latitude: Double,
    val longitude: Double
)

/**
 * Represents the means of transport that's being used.
 */
enum class RoutingProfile {

    /**
     * For car and motorcycle routing. This profile prefers high-speed roads like highways.
     */
    DRIVING,

    /**
     * For bicycle routing. This profile prefers routes that are safe for cyclist, avoiding highways and preferring streets with bike lanes.
     */
    CYCLING,

    /**
     * For pedestrian and hiking routing. This profile prefers sidewalks and trails.
     */
    WALKING,

    /**
     * For car and motorcycle routing. This profile factors in current and historic traffic conditions to avoid slowdowns.
     */
    DRIVING_TRAFFIC,
}

interface LocationHistoryData {
    val events: List<Location>
    val version: Int
}
