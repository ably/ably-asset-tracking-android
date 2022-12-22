package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.common.workerqueue.Worker
import com.ably.tracking.publisher.DefaultCorePublisher
import com.ably.tracking.publisher.PublisherInteractor
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification

internal class SetActiveTrackableWorker(
    private val trackable: Trackable,
    private val callbackFunction: ResultCallbackFunction<Unit>,
    private val publisherInteractor: PublisherInteractor,
    private val hooks: DefaultCorePublisher.Hooks
) : Worker<PublisherProperties, WorkerSpecification> {

    override fun doWork(
        properties: PublisherProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): PublisherProperties {
        if (properties.active != trackable) {
            properties.active = trackable

            // In the future consider moving following lines to handler
            hooks.trackables?.onActiveTrackableChanged(trackable)
            trackable.destination.let {
                if (it != null) {
                    publisherInteractor.setDestination(it, properties)
                } else {
                    publisherInteractor.removeCurrentDestination(properties)
                }
            }
        }
        callbackFunction(Result.success(Unit))
        return properties
    }

    override fun doWhenStopped(exception: Exception) {
        callbackFunction(Result.failure(exception))
    }
}
