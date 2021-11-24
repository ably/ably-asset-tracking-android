package com.ably.tracking.publisher.guards

import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.Trackable

/**
 * This guard class will keep track of trackables that are requested for removal. The aim of this class is to
 * maintain a set of trackables and expose client functions.
 */
internal class TrackableRemovalGuard {

    /**
     * A set of trackables that were marked for removal. This should be used to store / retrieve trackables
     * that did not finished adding.
     */
    private val trackables = hashMapOf<Trackable, MutableList<ResultCallbackFunction<Boolean>>>()

    fun markForRemoval(trackable: Trackable, callbackFunction: ResultCallbackFunction<Boolean>) {
        trackables[trackable]?.let {
            it.add(callbackFunction)
        } ?: kotlin.run {
            val handlers = mutableListOf(callbackFunction)
            trackables[trackable] = handlers
        }
    }

    fun isMarkedForRemoval(trackable: Trackable): Boolean = trackables.contains(trackable)

    fun removeMarked(trackable: Trackable, result: Result<Boolean>) {
        val handlers = trackables.remove(trackable)
        handlers?.forEach {
            it(result)
        }
    }

    fun clearAll() = trackables.clear()
}
