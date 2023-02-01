package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.ErrorInformation
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.workerqueue.Worker
import com.ably.tracking.publisher.PublisherInteractor
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.PublisherState
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification

internal class ConnectionReadyWorker(
    private val trackable: Trackable,
    private val ably: Ably,
    private val publisherInteractor: PublisherInteractor,
    private val channelStateChangeListener: ((connectionStateChange: ConnectionStateChange) -> Unit),
) : Worker<PublisherProperties, WorkerSpecification> {

    override fun doWork(
        properties: PublisherProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): PublisherProperties {
        if (properties.trackableRemovalGuard.isMarkedForRemoval(trackable)) {
            doAsyncWork {
                onTrackableRemovalRequested(properties, postWork)
            }
            return properties
        }

        setPublisherState(properties)
        subscribeForChannelStateChanges()
        startLocationUpdates(properties)

        return properties
    }

    /**
     * Set the publisher state to CONNECTED if we were previously CONNECTING.
     *
     * This indicates that we've done everything connection-wise for this trackable, so the next
     * trackable may now start adding in AddTrackableWorker.
     */
    private fun setPublisherState(properties: PublisherProperties) {
        if (properties.state == PublisherState.CONNECTING) {
            properties.state = PublisherState.CONNECTED
        }
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
        postWork(
            WorkerSpecification.TrackableRemovalRequested(trackable, Result.failure(exception))
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
