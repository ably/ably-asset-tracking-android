package com.ably.tracking.publisher.guards

import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.Trackable

/**
 * This guard class will keep track of trackables that are requested for removal. The aim of this class is to
 * maintain a set of trackables and expose client functions.
 */
internal interface TrackableRemovalGuard {
    fun markForRemoval(trackable: Trackable, callbackFunction: ResultCallbackFunction<Boolean>)
    fun isMarkedForRemoval(trackable: Trackable): Boolean
    fun removeMarked(trackable: Trackable, result: Result<Boolean>)
    fun clearAll()
}

internal class DefaultTrackableRemovalGuard : TrackableRemovalGuard {

    /**
     * A set of trackables that were marked for removal. This should be used to store / retrieve trackables
     * that did not finished adding.
     */
    private val trackables = hashMapOf<Trackable, MutableList<ResultCallbackFunction<Boolean>>>()

    override fun markForRemoval(trackable: Trackable, callbackFunction: ResultCallbackFunction<Boolean>) {
        trackables[trackable]?.let {
            it.add(callbackFunction)
        } ?: kotlin.run {
            val handlers = mutableListOf(callbackFunction)
            trackables[trackable] = handlers
        }
    }

    override fun isMarkedForRemoval(trackable: Trackable): Boolean = trackables.contains(trackable)

    override fun removeMarked(trackable: Trackable, result: Result<Boolean>) {
        val handlers = trackables.remove(trackable)
        handlers?.iterator()?.forEach {
            it(result)
        }
    }

    override fun clearAll() = trackables.clear()
}
