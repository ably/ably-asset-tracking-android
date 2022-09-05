package com.ably.tracking.publisher

import com.ably.tracking.Location
import com.ably.tracking.LocationUpdate

/**
 * Class responsible for storing locations for multiple trackables that are then used as the [LocationUpdate.skippedLocations].
 */
internal class SkippedLocations {
    /**
     * The maximum size for each individual list of skipped locations.
     */
    private val MAX_SKIPPED_LOCATIONS_SIZE = 60
    private val skippedLocations: MutableMap<String, MutableList<Location>> = mutableMapOf()

    /**
     * Adds a location to the list of skipped locations for the specified trackable.
     * If by adding this location the list exceeds its size limit, then the oldest location from the list is removed.
     *
     * @param trackableId The ID of the trackable.
     * @param location The location that will be added to the skipped locations list.
     */
    fun add(trackableId: String, location: Location) {
        val locations = skippedLocations[trackableId] ?: mutableListOf()
        locations.add(location)
        locations.sortBy { it.time }
        if (locations.size > MAX_SKIPPED_LOCATIONS_SIZE) {
            locations.removeFirst() // remove oldest location
        }
        skippedLocations[trackableId] = locations
    }

    /**
     * Adds locations from the [locations] list to the list of skipped locations for the specified trackable.
     * If by adding a location the list exceeds its size limit, then the oldest location from the list is removed.
     *
     * @param trackableId The ID of the trackable.
     * @param locations The list of locations that will be added to the skipped locations list.
     */
    fun addAll(trackableId: String, locations: List<Location>) {
        locations.forEach { add(trackableId, it) }
    }

    /**
     * Returns the skipped locations list sorted by time for the specified trackable.
     * If no locations are added for a trackable then it returns an empty list.
     *
     * @param trackableId The ID of the trackable.
     * @return Skipped locations list sorted by time or empty list if no locations were added.
     */
    fun toList(trackableId: String): List<Location> {
        return skippedLocations[trackableId] ?: emptyList()
    }

    /**
     * Clears the skipped locations list for the specified trackable ID.
     *
     * @param trackableId The ID of the trackable.
     */
    fun clear(trackableId: String) {
        skippedLocations[trackableId]?.clear()
    }

    /**
     * Clears all skipped locations lists.
     */
    fun clearAll() {
        skippedLocations.clear()
    }
}
