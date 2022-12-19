package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.common.workerqueue.Worker
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.PublisherState
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification

internal class AddTrackableFailedWorker(
    private val trackable: Trackable,
    private val callbackFunction: AddTrackableCallbackFunction,
    val exception: Exception,
    private val isConnectedToAbly: Boolean,
    private val ably: Ably,
) : Worker<PublisherProperties, WorkerSpecification> {

    override fun doWork(
        properties: PublisherProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): PublisherProperties {
        if (properties.state == PublisherState.CONNECTING) {
            properties.state = if (isConnectedToAbly) PublisherState.CONNECTED else PublisherState.IDLE
        }

        val failureResult = Result.failure<AddTrackableResult>(exception)
        callbackFunction(failureResult)
        properties.duplicateTrackableGuard.finishAddingTrackable(trackable, failureResult)
        properties.trackableRemovalGuard.removeMarked(trackable, Result.success(true))

        if (properties.hasNoTrackablesAddingOrAdded) {
            properties.state = PublisherState.DISCONNECTING
            doAsyncWork {
                ably.stopConnection()
                postWork(WorkerSpecification.StoppingConnectionFinished)
            }
        }

        return properties
    }

    override fun doWhenStopped(exception: Exception) {
        callbackFunction(Result.failure(exception))
    }
}
