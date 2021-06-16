package com.ably.tracking.publisher

import com.ably.tracking.Location

internal class SkippedLocations {
    private val MAX_SKIPPED_LOCATIONS_SIZE = 60
    private val skippedLocations: MutableMap<String, MutableList<Location>> = mutableMapOf()

    fun add(trackableId: String, location: Location) {
        val locations = skippedLocations[trackableId] ?: mutableListOf()
        locations.add(location)
        locations.sortBy { it.time }
        if (locations.size > MAX_SKIPPED_LOCATIONS_SIZE) {
            locations.removeFirst() // remove oldest location
        }
        skippedLocations[trackableId] = locations
    }

    fun toList(trackableId: String): List<Location> {
        return skippedLocations[trackableId] ?: emptyList()
    }

    fun clear(trackableId: String) {
        skippedLocations[trackableId]?.clear()
    }

    fun clearAll() {
        skippedLocations.clear()
    }
}
