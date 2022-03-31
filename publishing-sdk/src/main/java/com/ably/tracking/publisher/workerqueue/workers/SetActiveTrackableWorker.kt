package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.DefaultCorePublisher
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult

internal class SetActiveTrackableWorker(
    private val trackable: Trackable,
    private val callbackFunction: ResultCallbackFunction<Unit>,
    private val publisher: CorePublisher,
    private val hooks: DefaultCorePublisher.Hooks
) : Worker {
    override fun doWork(properties: PublisherProperties): SyncAsyncResult {
        if (properties.active != trackable) {
            properties.active = trackable

            // In the future consider moving following lines to handler
            hooks.trackables?.onActiveTrackableChanged(trackable)
            trackable.destination.let {
                if (it != null) {
                    publisher.setDestination(it, properties)
                } else {
                    publisher.removeCurrentDestination(properties)
                }
            }
        }
        callbackFunction(Result.success(Unit))
        return SyncAsyncResult()
    }

    override fun doWhenStopped(exception: Exception) {
        callbackFunction(Result.failure(exception))
    }
}
