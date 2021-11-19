package com.ably.tracking.publisher.guards

import com.ably.tracking.publisher.AddTrackableHandler
import com.ably.tracking.publisher.AddTrackableResult
import com.ably.tracking.publisher.Trackable

/**
 * Class that protects from adding duplicates of a trackable that is currently being added to the publisher.
 * This class is not safe to access from multiple threads at the same time.
 */
class DuplicateTrackableGuard {
    /**
     * Stores trackables that are currently being added (the adding process has started but hasn't finished yet).
     */
    private val trackablesCurrentlyBeingAdded: MutableSet<Trackable> = mutableSetOf()

    /**
     * Stores handlers from trackables that are duplicates of the trackables from [trackablesCurrentlyBeingAdded].
     */
    private val duplicateAddCallsHandlers: MutableMap<Trackable, MutableList<AddTrackableHandler>> = mutableMapOf()

    /**
     * Marks that the specified trackable adding process has started.
     *
     * @param trackable The trackable that's being added.
     */
    fun startAddingTrackable(trackable: Trackable) {
        trackablesCurrentlyBeingAdded.add(trackable)
    }

    /**
     * Marks that the specified trackable adding process has finished with either success or failure.
     * Notifies handlers of the duplicate add calls for this trackable if there were any.
     *
     * @param trackable The trackable that was being added.
     * @param result The result of the adding process.
     */
    fun finishAddingTrackable(trackable: Trackable, result: Result<AddTrackableResult>) {
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
    fun isCurrentlyAddingTrackable(trackable: Trackable): Boolean {
        return trackablesCurrentlyBeingAdded.contains(trackable)
    }

    /**
     * Saves the handler from a duplicate add call for a specified trackable.
     * This handler will be called when the original trackable adding process will finish in [finishAddingTrackable].
     *
     * @param trackable The duplicate trackable.
     * @param handler The handler of the duplicate trackable adding process.
     */
    fun saveDuplicateAddHandler(trackable: Trackable, handler: AddTrackableHandler) {
        val handlers = duplicateAddCallsHandlers[trackable] ?: mutableListOf()
        handlers.add(handler)
        duplicateAddCallsHandlers[trackable] = handlers
    }

    /**
     * Clears the state for the specified trackable.
     *
     * @param trackable The trackable to clear.
     */
    fun clear(trackable: Trackable) {
        trackablesCurrentlyBeingAdded.remove(trackable)
        duplicateAddCallsHandlers.remove(trackable)
    }

    /**
     * Clears the state for all trackables.
     */
    fun clearAll() {
        trackablesCurrentlyBeingAdded.clear()
        duplicateAddCallsHandlers.clear()
    }
}
