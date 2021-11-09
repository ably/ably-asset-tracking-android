package com.ably.tracking.publisher.guards

import com.ably.tracking.publisher.Trackable

/**
 * This guard class will keep track of trackables that are requested for removal. The aim of this class is to
 * maintain a set of trackables and expose client functions.
 */
class TrackableRemovalGuard {

    /**
     * A set of trackables that were marked for removal. This should be used to store / retrieve trackables
     * that did not finished adding.
     */
    private val trackables: MutableSet<Trackable> = mutableSetOf()

    fun markForRemoval(trackable: Trackable) = trackables.add(trackable)

    fun isMarkedForRemoval(trackable: Trackable): Boolean = trackables.contains(trackable)

    fun removeMarked(trackable: Trackable) = trackables.remove(trackable)

    fun clearAll() = trackables.clear()
}
