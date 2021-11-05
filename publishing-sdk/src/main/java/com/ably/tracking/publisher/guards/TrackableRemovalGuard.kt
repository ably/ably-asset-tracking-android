package com.ably.tracking.publisher.guards

import com.ably.tracking.publisher.Trackable

class TrackableRemovalGuard {

    /**
     * A set of trackables that were marked for removal. This should be used to store / retrieve trackables
     * that did not finished adding.
     */
    private val trackablesMarkedForRemoval: MutableSet<Trackable> = mutableSetOf()

    fun markForRemoval(trackable: Trackable) {
        trackablesMarkedForRemoval.add(trackable)
    }

    fun markedForRemoval(trackable: Trackable):Boolean = trackablesMarkedForRemoval.contains(trackable)
}