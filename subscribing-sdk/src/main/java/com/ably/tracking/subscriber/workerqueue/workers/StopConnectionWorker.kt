package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.ConnectionException
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.subscriber.SubscriberInteractor
import com.ably.tracking.subscriber.Properties
import com.ably.tracking.subscriber.workerqueue.Worker
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification
import kotlinx.coroutines.runBlocking

internal class StopConnectionWorker(
    private val ably: Ably,
    private val subscriberInteractor: SubscriberInteractor,
    private val callbackFunction: ResultCallbackFunction<Unit>
) : Worker {
    override fun doWork(
        properties: Properties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ) {
        // We're using [runBlocking] on purpose as we want to block the whole subscriber when it's stopping.
        runBlocking {
            try {
                ably.close(properties.presenceData)
                properties.isStopped = true
                subscriberInteractor.notifyAssetIsOffline()
                callbackFunction(Result.success(Unit))
            } catch (exception: ConnectionException) {
                callbackFunction(Result.failure(exception))
            }
        }
    }

    override fun doWhenStopped(exception: Exception) {
        callbackFunction(Result.success(Unit))
    }
}
