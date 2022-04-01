package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.ConnectionException
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

internal class StopWorker(
    private val callbackFunction: ResultCallbackFunction<Unit>,
    private val ably: Ably,
    private val corePublisher: CorePublisher,
    private val timeoutInMilliseconds: Long,
) : Worker {
    override fun doWork(properties: PublisherProperties): SyncAsyncResult {
        // We're using [runBlocking] on purpose as we want to block the whole publisher when it's stopping.
        runBlocking {
            try {
                withTimeout(timeoutInMilliseconds) {
                    if (properties.isTracking) {
                        corePublisher.stopLocationUpdates(properties)
                    }
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
        properties.isStopped = true
        return SyncAsyncResult()
    }

    override fun doWhenStopped(exception: Exception) {
        // If we are already stopped the stop worker should return success
        callbackFunction(Result.success(Unit))
    }
}
