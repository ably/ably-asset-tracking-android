package com.ably.tracking.publisher

/**
 * Class responsible for managing state connected to location updates that are going to or are being published.
 */
internal class LocationsPublishingState {
    /**
     * The maximum number of retries after a location update is considered to be failed.
     */
    private val MAX_RETRY_COUNT = 1

    /**
     * Stores information about trackables that have currently pending messages.
     */
    private val pendingMessages: MutableSet<String> = mutableSetOf()

    /**
     * Stores location updates that are waiting to be processed, for each trackable independently.
     */
    private val waitingLocationUpdates: MutableMap<String, MutableList<EnhancedLocationChangedEvent>> = mutableMapOf()

    /**
     * Stores the number of retries of the current location update, for each trackable independently.
     */
    private val retryCounter: MutableMap<String, Int> = mutableMapOf()

    /**
     * Marks that the specified trackable has a pending message.
     *
     * @param trackableId The ID of the trackable.
     */
    fun markMessageAsPending(trackableId: String) {
        pendingMessages.add(trackableId)
    }

    /**
     * Marks that the specified trackable does not have a pending message.
     *
     * @param trackableId The ID of the trackable.
     */
    fun unmarkMessageAsPending(trackableId: String) {
        pendingMessages.remove(trackableId)
        resetRetryCount(trackableId)
    }

    /**
     * Checks if the specified trackable has a pending message.
     *
     * @param trackableId The ID of the trackable.
     * @return true if trackable has pending message, false otherwise.
     */
    fun hasPendingMessage(trackableId: String): Boolean =
        pendingMessages.contains(trackableId)

    /**
     * Adds the event to the waiting list for the specified trackable.
     *
     * @param trackableId The ID of the trackable.
     * @param enhancedLocationChangedEvent The event that will be added to waiting list.
     */
    fun addToWaiting(trackableId: String, enhancedLocationChangedEvent: EnhancedLocationChangedEvent) {
        if (waitingLocationUpdates[trackableId] == null) {
            waitingLocationUpdates[trackableId] = mutableListOf()
        }
        waitingLocationUpdates[trackableId]!!.add(enhancedLocationChangedEvent)
    }

    /**
     * Returns the next event that's waiting to be processed.
     *
     * @param trackableId The ID of the trackable.
     * @return The next waiting event or null if no events are waiting.
     */
    fun getNextWaiting(trackableId: String): EnhancedLocationChangedEvent? =
        waitingLocationUpdates[trackableId]?.removeFirstOrNull()

    /**
     * Checks if sending of the current location update for the specified trackable should be retried.
     *
     * @param trackableId The ID of the trackable.
     * @return true if should retry publishing, false otherwise.
     */
    fun shouldRetryPublishing(trackableId: String): Boolean =
        getRetryCount(trackableId) < MAX_RETRY_COUNT

    /**
     * Increments the retry counter for the current location update for the specified trackable.
     *
     * @param trackableId The ID of the trackable.
     */
    fun incrementRetryCount(trackableId: String) {
        retryCounter[trackableId] = getRetryCount(trackableId) + 1
    }

    private fun resetRetryCount(trackableId: String) {
        retryCounter[trackableId] = 0
    }

    private fun getRetryCount(trackableId: String): Int =
        retryCounter[trackableId] ?: 0

    /**
     * Clears the state for the specified trackable.
     *
     * @param trackableId The ID of the trackable.
     */
    fun clear(trackableId: String) {
        pendingMessages.remove(trackableId)
        waitingLocationUpdates.remove(trackableId)
        retryCounter.remove(trackableId)
    }

    /**
     * Clears the state for all trackables.
     */
    fun clearAll() {
        pendingMessages.clear()
        waitingLocationUpdates.clear()
        retryCounter.clear()
    }
}
