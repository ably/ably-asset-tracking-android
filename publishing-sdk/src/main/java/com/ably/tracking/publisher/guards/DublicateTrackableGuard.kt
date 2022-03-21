package com.ably.tracking.publisher.guards

import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.workers.AddTrackableCallbackFunction
import com.ably.tracking.publisher.workerqueue.workers.AddTrackableResult

/**
 * Interface for [DublicateTrackableGuardImpl] is created to make it possible to create test fakes.
 * */
internal interface DuplicateTrackableGuard {
    fun startAddingTrackable(trackable: Trackable)
    fun finishAddingTrackable(trackable: Trackable, result: Result<AddTrackableResult>)
    fun isCurrentlyAddingTrackable(trackable: Trackable): Boolean
    fun saveDuplicateAddHandler(trackable: Trackable, callbackFunction: AddTrackableCallbackFunction)
    fun clear(trackable: Trackable)
    fun clearAll()
}

/**
 * Class that protects from adding duplicates of a trackable that is currently being added to the publisher.
 * This class is not safe to access from multiple threads at the same time.
 */
internal class DublicateTrackableGuardImpl : DuplicateTrackableGuard {
    /**
     * Stores trackables that are currently being added (the adding process has started but hasn't finished yet).
     */
    private val trackablesCurrentlyBeingAdded: MutableSet<Trackable> = mutableSetOf()

    /**
     * Stores handlers from trackables that are duplicates of the trackables from [trackablesCurrentlyBeingAdded].
     */
    private val duplicateAddCallsHandlers: MutableMap<Trackable, MutableList<AddTrackableCallbackFunction>> = mutableMapOf()

    /**
     * Marks that the specified trackable adding process has started.
     *
     * @param trackable The trackable that's being added.
     */
    override fun startAddingTrackable(trackable: Trackable) {
        trackablesCurrentlyBeingAdded.add(trackable)
    }

    /**
     * Marks that the specified trackable adding process has finished with either success or failure.
     * Notifies handlers of the duplicate add calls for this trackable if there were any.
     *
     * @param trackable The trackable that was being added.
     * @param result The result of the adding process.
     */
    override fun finishAddingTrackable(trackable: Trackable, result: Result<AddTrackableResult>) {
        trackablesCurrentlyBeingAdded.remove(trackable)
        duplicateAddCallsHandlers[trackable]?.forEach { handler -> handler(result) }
        duplicateAddCallsHandlers[trackable]?.clear()
    }

    /**
     * Checks if the adding process for the specified trackable is already ongoing.
     *
     * @param trackable The trackable that's being added.
     * @return True if the specified trackable is currently being added, false otherwise.
     */
    override fun isCurrentlyAddingTrackable(trackable: Trackable): Boolean {
        return trackablesCurrentlyBeingAdded.contains(trackable)
    }

    /**
     * Saves the handler from a duplicate add call for a specified trackable.
     * This handler will be called when the original trackable adding process will finish in [finishAddingTrackable].
     *
     * @param trackable The duplicate trackable.
     * @param callbackFunction The handler of the duplicate trackable adding process.
     */
    override fun saveDuplicateAddHandler(trackable: Trackable, callbackFunction: AddTrackableCallbackFunction) {
        val handlers = duplicateAddCallsHandlers[trackable] ?: mutableListOf()
        handlers.add(callbackFunction)
        duplicateAddCallsHandlers[trackable] = handlers
    }

    /**
     * Clears the state for the specified trackable.
     *
     * @param trackable The trackable to clear.
     */
    override fun clear(trackable: Trackable) {
        trackablesCurrentlyBeingAdded.remove(trackable)
        duplicateAddCallsHandlers.remove(trackable)
    }

    /**
     * Clears the state for all trackables.
     */
    override fun clearAll() {
        trackablesCurrentlyBeingAdded.clear()
        duplicateAddCallsHandlers.clear()
    }
}
