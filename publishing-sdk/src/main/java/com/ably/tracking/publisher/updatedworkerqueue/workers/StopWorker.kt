package com.ably.tracking.publisher.updatedworkerqueue.workers

import com.ably.tracking.ConnectionException
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.common.workerqueue.Worker
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.PublisherState
import com.ably.tracking.publisher.updatedworkerqueue.WorkerSpecification
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

internal class StopWorker(
    private val callbackFunction: ResultCallbackFunction<Unit>,
    private val ably: Ably,
    private val corePublisher: CorePublisher,
    private val timeoutInMilliseconds: Long,
) : Worker<PublisherProperties, WorkerSpecification> {

    override fun doWork(
        properties: PublisherProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): PublisherProperties {
        ably
        corePublisher
        timeoutInMilliseconds
        // We're using [runBlocking] on purpose as we want to block the whole publisher when it's stopping.
        runBlocking {
            try {
                withTimeout(timeoutInMilliseconds) {
                    if (properties.isTracking) {
                        corePublisher.stopLocationUpdates(properties)
                    }
                    corePublisher.closeMapbox()
                    ably.close(properties.presenceData)
                    properties.dispose()
                    callbackFunction(Result.success(Unit))
                }
            } catch (exception: ConnectionException) {
                callbackFunction(Result.failure(exception))
            } catch (exception: TimeoutCancellationException) {
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