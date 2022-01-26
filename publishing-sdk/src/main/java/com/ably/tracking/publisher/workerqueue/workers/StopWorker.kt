package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.ConnectionException
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.Event
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.StopEvent
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult
import kotlinx.coroutines.runBlocking

internal class StopWorker(
    private val callbackFunction: ResultCallbackFunction<Unit>,
    private val ably: Ably,
    private val corePublisher: CorePublisher
) : Worker {
    override val event: Event
        get() = StopEvent(callbackFunction)

    override fun doWork(properties: PublisherProperties): SyncAsyncResult {
        // We're using [runBlocking] on purpose as we want to block the whole publisher when it's stopping.
        runBlocking {
            if (properties.isTracking) {
                corePublisher.stopLocationUpdates(properties)
            }
            try {
                ably.close(properties.presenceData)
                properties.dispose()
                properties.isStopped = true
                callbackFunction(Result.success(Unit))
            } catch (exception: ConnectionException) {
                callbackFunction(Result.failure(exception))
            }
        }
        return SyncAsyncResult()
    }
}
