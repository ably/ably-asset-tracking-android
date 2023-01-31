package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.ErrorInformation
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.workerqueue.Worker
import com.ably.tracking.publisher.PublisherInteractor
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification

internal class ConnectionReadyWorker(
    private val trackable: Trackable,
    private val ably: Ably,
    private val publisherInteractor: PublisherInteractor,
    private val channelStateChangeListener: ((connectionStateChange: ConnectionStateChange) -> Unit),
    private val isSubscribedToPresence: Boolean,
    private val presenceUpdateListener: ((presenceMessage: com.ably.tracking.common.PresenceMessage) -> Unit),
) : Worker<PublisherProperties, WorkerSpecification> {
    /**
     * Whether the trackable is being removed.
     * Used to properly handle unexpected exceptions in [onUnexpectedAsyncError].
     */
    private var isBeingRemoved = false

    override fun doWork(
        properties: PublisherProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): PublisherProperties {
        if (properties.trackableRemovalGuard.isMarkedForRemoval(trackable)) {
            doAsyncWork {
                isBeingRemoved = true
                onTrackableRemovalRequested(properties, postWork)
            }
            return properties
        }

        subscribeForChannelStateChanges()
        startLocationUpdates(properties)

        if (!isSubscribedToPresence) {
            postWork(WorkerSpecification.RetrySubscribeToPresence(trackable, presenceUpdateListener))
        } else {
            properties.trackableSubscribedToPresenceFlags[trackable.id] = true
        }

        if (properties.trackableEnteredPresenceFlags[trackable.id] != true) {
            postWork(WorkerSpecification.RetryEnterPresence(trackable))
        }

        publisherInteractor.updateTrackableState(properties, trackable.id)

        return properties
    }

    private suspend fun onTrackableRemovalRequested(
        properties: PublisherProperties,
        postWork: (WorkerSpecification) -> Unit
    ) {
        ably.disconnect(trackable.id, properties.presenceData)
        postWork(WorkerSpecification.TrackableRemovalRequested(trackable, Result.success(Unit)))
    }

    private fun subscribeForChannelStateChanges() {
        ably.subscribeForChannelStateChange(trackable.id) {
            channelStateChangeListener(it)
        }
    }

    private fun startLocationUpdates(properties: PublisherProperties) {
        if (!properties.isTracking) {
            publisherInteractor.startLocationUpdates(properties)
        }
    }

    override fun onUnexpectedAsyncError(exception: Exception, postWork: (WorkerSpecification) -> Unit) {
        if (isBeingRemoved) {
            postWork(
                WorkerSpecification.TrackableRemovalRequested(trackable, Result.failure(exception))
            )
            return
        }

        postWork(
            WorkerSpecification.FailTrackable(
                trackable,
                ErrorInformation("Unexpected async error on connection ready: $exception")
            )
        )
    }

    override fun doWhenStopped(exception: Exception) {
        // No op
    }

    override fun onUnexpectedError(exception: Exception, postWork: (WorkerSpecification) -> Unit) {
        postWork(
            WorkerSpecification.FailTrackable(
                trackable,
                ErrorInformation("Unexpected error on connection ready: $exception")
            )
        )
    }
}
