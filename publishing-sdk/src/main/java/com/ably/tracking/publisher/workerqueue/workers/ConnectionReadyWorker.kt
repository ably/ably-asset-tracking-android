package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.workerqueue.Worker
import com.ably.tracking.publisher.DefaultCorePublisher
import com.ably.tracking.publisher.PublisherInteractor
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification

internal class ConnectionReadyWorker(
    private val trackable: Trackable,
    private val callbackFunction: AddTrackableCallbackFunction,
    private val ably: Ably,
    private val hooks: DefaultCorePublisher.Hooks,
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

    override fun doWhenStopped(exception: Exception) {
        callbackFunction(Result.failure(exception))
    }

    private suspend fun onTrackableRemovalRequested(
        properties: PublisherProperties,
        postWork: (WorkerSpecification) -> Unit
    ) {
        ably.disconnect(trackable.id, properties.presenceData)
        postWork(WorkerSpecification.TrackableRemovalRequested(trackable, callbackFunction, Result.success(Unit)))
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

    override fun onUnexpectedError(exception: Exception, postWork: (WorkerSpecification) -> Unit) {
        callbackFunction(Result.failure(exception))
    }

    override fun onUnexpectedAsyncError(exception: Exception, postWork: (WorkerSpecification) -> Unit) {
        if (isBeingRemoved) {
            postWork(
                WorkerSpecification.TrackableRemovalRequested(trackable, callbackFunction, Result.failure(exception))
            )
        } else {
            callbackFunction(Result.failure(exception))
        }
    }
}
