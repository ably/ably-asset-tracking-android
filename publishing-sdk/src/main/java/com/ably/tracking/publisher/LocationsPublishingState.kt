package com.ably.tracking.publisher

internal class LocationsPublishingState {
    private val MAX_RETRY_COUNT = 1
    private val pendingMessages: MutableSet<String> = mutableSetOf()
    private val waitingLocationUpdates: MutableMap<String, MutableList<EnhancedLocationChangedEvent>> = mutableMapOf()
    private val retryCounter: MutableMap<String, Int> = mutableMapOf()

    fun markMessageAsPending(trackableId: String) {
        pendingMessages.add(trackableId)
    }

    fun unmarkMessageAsPending(trackableId: String) {
        pendingMessages.remove(trackableId)
        resetRetryCount(trackableId)
    }

    fun hasPendingMessage(trackableId: String): Boolean =
        pendingMessages.contains(trackableId)

    fun addToWaiting(trackableId: String, enhancedLocationChangedEvent: EnhancedLocationChangedEvent) {
        if (waitingLocationUpdates[trackableId] == null) {
            waitingLocationUpdates[trackableId] = mutableListOf()
        }
        waitingLocationUpdates[trackableId]!!.add(enhancedLocationChangedEvent)
    }

    fun getNextWaiting(trackableId: String): EnhancedLocationChangedEvent? =
        waitingLocationUpdates[trackableId]?.removeFirstOrNull()

    fun shouldRetryPublishing(trackableId: String): Boolean =
        getRetryCount(trackableId) < MAX_RETRY_COUNT

    fun incrementRetryCount(trackableId: String) {
        retryCounter[trackableId] = getRetryCount(trackableId) + 1
    }

    private fun resetRetryCount(trackableId: String) {
        retryCounter[trackableId] = 0
    }

    private fun getRetryCount(trackableId: String): Int =
        retryCounter[trackableId] ?: 0

    fun clear(trackableId: String) {
        pendingMessages.remove(trackableId)
        waitingLocationUpdates.remove(trackableId)
        retryCounter.remove(trackableId)
    }

    fun clearAll() {
        pendingMessages.clear()
        waitingLocationUpdates.clear()
        retryCounter.clear()
    }
}
