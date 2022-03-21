package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult

internal class DisconnectSuccessWorker(
    private val trackable: Trackable,
    private val callbackFunction: ResultCallbackFunction<Unit>,
    private val corePublisher: CorePublisher,
    private val shouldRecalculateResolutionCallback: () -> Unit,
) : Worker {
    override fun doWork(properties: PublisherProperties): SyncAsyncResult {
        properties.trackables.remove(trackable)

        corePublisher.updateTrackables(properties)
        properties.trackableStateFlows.remove(trackable.id) // there is no way to stop the StateFlow so we just remove it
        corePublisher.updateTrackableStateFlows(properties)
        properties.trackableStates.remove(trackable.id)

        corePublisher.notifyResolutionPolicyThatTrackableWasRemoved(trackable)
        corePublisher.removeAllSubscribers(trackable, properties)

        properties.resolutions.remove(trackable.id)
            ?.let { shouldRecalculateResolutionCallback() }
        properties.requests.remove(trackable.id)

        properties.lastSentEnhancedLocations.remove(trackable.id)
        properties.lastSentRawLocations.remove(trackable.id)
        properties.skippedEnhancedLocations.clear(trackable.id)
        properties.skippedRawLocations.clear(trackable.id)

        properties.enhancedLocationsPublishingState.clear(trackable.id)
        properties.rawLocationsPublishingState.clear(trackable.id)

        properties.duplicateTrackableGuard.clear(trackable)

        // If this was the active Trackable then clear that state and remove destination.
        if (properties.active == trackable) {
            corePublisher.removeCurrentDestination(properties)
            properties.active = null
            corePublisher.notifyResolutionPolicyThatActiveTrackableHasChanged(null)
        }

        // When we remove the last trackable then we should stop location updates
        if (properties.trackables.isEmpty() && properties.isTracking) {
            corePublisher.stopLocationUpdates(properties)
        }

        properties.lastChannelConnectionStateChanges.remove(trackable.id)

        callbackFunction(Result.success(Unit))

        return SyncAsyncResult()
    }

    override fun doWhenStopped(exception: Exception) {
        callbackFunction(Result.failure(exception))
    }
}
