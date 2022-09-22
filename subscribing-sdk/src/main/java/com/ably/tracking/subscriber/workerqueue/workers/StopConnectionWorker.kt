package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.ConnectionException
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.subscriber.Core
import com.ably.tracking.subscriber.Properties
import com.ably.tracking.subscriber.workerqueue.Worker
import com.ably.tracking.subscriber.workerqueue.WorkerParams

internal class StopConnectionWorker(
    private val ably: Ably,
    private val core: Core,
    private val callbackFunction: ResultCallbackFunction<Unit>
) : Worker {
    override fun doWork(
        properties: Properties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerParams) -> Unit
    ) {
        //TODO use runBlocking instead
        doAsyncWork {
            try {
                ably.close(properties.presenceData)
                properties.isStopped = true
                core.notifyAssetIsOffline()
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
