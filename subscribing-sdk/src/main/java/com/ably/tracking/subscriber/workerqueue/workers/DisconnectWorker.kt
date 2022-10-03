package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.subscriber.Properties
import com.ably.tracking.subscriber.workerqueue.Worker
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification

internal class DisconnectWorker(
    private val ably: Ably,
    private val trackableId: String,
    private val callbackFunction: () -> Unit
) : Worker {

    override fun doWork(
        properties: Properties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): Properties {
        doAsyncWork {
            ably.disconnect(trackableId, properties.presenceData)
            callbackFunction()
        }
        return properties
    }

    override fun doWhenStopped(exception: Exception) {
        callbackFunction()
    }
}
