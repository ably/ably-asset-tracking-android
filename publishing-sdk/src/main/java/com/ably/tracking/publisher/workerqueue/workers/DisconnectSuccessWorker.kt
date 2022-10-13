package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.results.DisconnectSuccessWorkResult
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult

internal class DisconnectSuccessWorker(
    private val trackable: Trackable,
    private val callbackFunction: ResultCallbackFunction<Unit>,
    private val corePublisher: CorePublisher,
    private val shouldRecalculateResolutionCallback: () -> Unit,
    private val ably: Ably,
) : Worker {
    override fun doWork(properties: PublisherProperties): SyncAsyncResult {
        removeTrackable(properties)
        removeTrackableState(properties)
        updateResolutions(properties)
        clearSubscribersData(properties)
        clearLocationUpdatesData(properties)
        if (isRemovedTrackableTheActiveOne(properties)) {
            clearActiveTrackableState(properties)
        }
        if (isRemovedTrackableTheLastOne(properties)) {
            stopLocationUpdates(properties)
        }
        notifyRemoveOperationFinished()

        val removedTheLastTrackable = properties.hasNoTrackablesAddingOrAdded
        if (removedTheLastTrackable) {
            properties.isStoppingAbly = true
            return SyncAsyncResult(asyncWork = {
                ably.stopConnection()
                DisconnectSuccessWorkResult.StopConnectionCompleted
            })
        }

        return SyncAsyncResult()
    }

    override fun doWhenStopped(exception: Exception) {
        callbackFunction(Result.failure(exception))
    }

    private fun removeTrackable(properties: PublisherProperties) {
        properties.trackables.remove(trackable)
        corePublisher.updateTrackables(properties)
        properties.duplicateTrackableGuard.clear(trackable)
    }

    private fun removeTrackableState(properties: PublisherProperties) {
        properties.trackableStateFlows.remove(trackable.id) // there is no way to stop the StateFlow so we just remove it
        corePublisher.updateTrackableStateFlows(properties)
        properties.trackableStates.remove(trackable.id)
        properties.lastChannelConnectionStateChanges.remove(trackable.id)
        properties.trackableSubscribedToPresenceFlags.remove(trackable.id)
    }

    private fun updateResolutions(properties: PublisherProperties) {
        corePublisher.notifyResolutionPolicyThatTrackableWasRemoved(trackable)
        properties.resolutions.remove(trackable.id)
            ?.let { shouldRecalculateResolutionCallback() }
    }

    private fun clearSubscribersData(properties: PublisherProperties) {
        corePublisher.removeAllSubscribers(trackable, properties)
        properties.requests.remove(trackable.id)
    }

    private fun clearLocationUpdatesData(properties: PublisherProperties) {
        properties.lastSentEnhancedLocations.remove(trackable.id)
        properties.lastSentRawLocations.remove(trackable.id)
        properties.skippedEnhancedLocations.clear(trackable.id)
        properties.skippedRawLocations.clear(trackable.id)
        properties.enhancedLocationsPublishingState.clear(trackable.id)
        properties.rawLocationsPublishingState.clear(trackable.id)
    }

    private fun isRemovedTrackableTheActiveOne(properties: PublisherProperties): Boolean =
        properties.active == trackable

    private fun clearActiveTrackableState(properties: PublisherProperties) {
        corePublisher.removeCurrentDestination(properties)
        properties.active = null
        corePublisher.notifyResolutionPolicyThatActiveTrackableHasChanged(null)
    }

    private fun isRemovedTrackableTheLastOne(properties: PublisherProperties): Boolean =
        properties.trackables.isEmpty()

    private fun stopLocationUpdates(properties: PublisherProperties) {
        if (properties.isTracking) {
            corePublisher.stopLocationUpdates(properties)
        }
    }

    private fun notifyRemoveOperationFinished() {
        callbackFunction(Result.success(Unit))
    }
}
