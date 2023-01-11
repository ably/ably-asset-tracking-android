package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.ConnectionException
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.common.workerqueue.CallbackWorker
import com.ably.tracking.publisher.PublisherInteractor
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.PublisherState
import com.ably.tracking.publisher.workerqueue.WorkerSpecification
import kotlinx.coroutines.runBlocking

internal class StopWorker(
    callbackFunction: ResultCallbackFunction<Unit>,
    private val ably: Ably,
    private val publisherInteractor: PublisherInteractor,
) : CallbackWorker<PublisherProperties, WorkerSpecification>(callbackFunction) {

    override fun doWork(
        properties: PublisherProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): PublisherProperties {
        ably
        publisherInteractor
        // We're using [runBlocking] on purpose as we want to block the whole publisher when it's stopping.
        runBlocking {
            try {
                if (properties.isTracking) {
                    publisherInteractor.stopLocationUpdates(properties)
                }
                publisherInteractor.closeMapbox()
                ably.close(properties.presenceData)
                properties.dispose()
                callbackFunction(Result.success(Unit))
            } catch (exception: ConnectionException) {
                callbackFunction(Result.failure(exception))
            }
        }
        // We should mark the publisher as stopped no matter if the whole stopping process completed successfully.
        properties.state = PublisherState.STOPPED
        return properties
    }

    override fun doWhenStopped(exception: Exception) {
        // If we are already stopped the stop worker should return success
        callbackFunction(Result.success(Unit))
    }
}
