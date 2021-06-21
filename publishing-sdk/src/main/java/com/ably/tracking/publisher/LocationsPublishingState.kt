package com.ably.tracking.publisher

internal class LocationsPublishingState {
    private val pendingMessages: MutableSet<String> = mutableSetOf()
    private val waitingLocationUpdates: MutableMap<String, MutableList<EnhancedLocationChangedEvent>> = mutableMapOf()

    fun markMessageAsPending(trackableId: String) {
        pendingMessages.add(trackableId)
    }

    fun unmarkMessageAsPending(trackableId: String) {
        pendingMessages.remove(trackableId)
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

    fun clear(trackableId: String) {
        pendingMessages.remove(trackableId)
        waitingLocationUpdates.remove(trackableId)
    }

    fun clearAll() {
        pendingMessages.clear()
        waitingLocationUpdates.clear()
    }
}
