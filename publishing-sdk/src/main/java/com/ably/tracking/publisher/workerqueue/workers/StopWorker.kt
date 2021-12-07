package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.ConnectionException
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.Event
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.StopEvent
import com.ably.tracking.publisher.workerqueue.results.StopResult
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult

internal class StopWorker(
    private val callbackFunction: ResultCallbackFunction<Unit>,
    private val ably: Ably,
    private val corePublisher: CorePublisher
) : Worker {
    override val event: Event
        get() = StopEvent(callbackFunction)

    override suspend fun doWork(properties: PublisherProperties): SyncAsyncResult {
        if (properties.isTracking) {
            corePublisher.stopLocationUpdates(properties)
        }
        return try {
            ably.close(properties.presenceData)
            properties.dispose()
            properties.isStopped = true
            SyncAsyncResult(StopResult.Success(callbackFunction))
        } catch (exception: ConnectionException) {
            SyncAsyncResult(StopResult.Fail(callbackFunction, exception))
        }
    }
}
