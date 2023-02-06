package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.common.workerqueue.Worker
import com.ably.tracking.publisher.PublisherInteractor
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.PublisherState
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification

internal class DisconnectSuccessWorker(
    private val trackable: Trackable,
    private val publisherInteractor: PublisherInteractor,
    private val shouldRecalculateResolutionCallback: () -> Unit,
    private val ably: Ably,
) : Worker<PublisherProperties, WorkerSpecification> {
    /**
     * Whether the worker is also performing disconnecting.
     * Used to properly handle unexpected exceptions in [onUnexpectedAsyncError].
     */
    private var isDisconnecting: Boolean = false

    override fun doWork(
        properties: PublisherProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): PublisherProperties {
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
        notifyRemoveOperationFinished(properties)

        val removedTheLastTrackable = properties.hasNoTrackablesAdded
        if (removedTheLastTrackable) {
            properties.state = PublisherState.DISCONNECTING
            doAsyncWork {
                isDisconnecting = true
                ably.stopConnection()
                postWork(WorkerSpecification.StoppingConnectionFinished)
            }
        }

        return properties
    }

    private fun removeTrackable(properties: PublisherProperties) {
        properties.trackables.remove(trackable)
        publisherInteractor.updateTrackables(properties)
    }

    private fun removeTrackableState(properties: PublisherProperties) {
        properties.trackableStateFlows.remove(trackable.id) // there is no way to stop the StateFlow so we just remove it
        publisherInteractor.updateTrackableStateFlows(properties)
        properties.trackableStates.remove(trackable.id)
        properties.lastChannelConnectionStateChanges.remove(trackable.id)
        properties.trackableSubscribedToPresenceFlags.remove(trackable.id)
        properties.trackableEnteredPresenceFlags.remove(trackable.id)
        properties.trackablesWithFinalStateSet.remove(trackable.id)
    }

    private fun updateResolutions(properties: PublisherProperties) {
        publisherInteractor.notifyResolutionPolicyThatTrackableWasRemoved(trackable)
        properties.resolutions.remove(trackable.id)
            ?.let { shouldRecalculateResolutionCallback() }
    }

    private fun clearSubscribersData(properties: PublisherProperties) {
        publisherInteractor.removeAllSubscribers(trackable, properties)
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
        publisherInteractor.removeCurrentDestination(properties)
        properties.active = null
        publisherInteractor.notifyResolutionPolicyThatActiveTrackableHasChanged(null)
    }

    private fun isRemovedTrackableTheLastOne(properties: PublisherProperties): Boolean =
        properties.trackables.isEmpty()

    private fun stopLocationUpdates(properties: PublisherProperties) {
        if (properties.isTracking) {
            publisherInteractor.stopLocationUpdates(properties)
        }
    }

    private fun notifyRemoveOperationFinished(properties: PublisherProperties) {
        properties.trackableRemovalGuard.removeMarked(trackable, Result.success(true))
    }

    override fun onUnexpectedAsyncError(exception: Exception, postWork: (WorkerSpecification) -> Unit) {
        // Async work is an optional step that happens after the callback was called so it should not call the callback
        if (isDisconnecting) {
            // When async work fails we should make sure that the SDK state is not stuck in DISCONNECTING so we post a new worker
            postWork(WorkerSpecification.StoppingConnectionFinished)
        }
    }

    override fun doWhenStopped(exception: Exception) {
        // No op
    }

    override fun onUnexpectedError(exception: Exception, postWork: (WorkerSpecification) -> Unit) {
        // No op
    }
}
