package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.subscriber.SubscriberProperties
import com.ably.tracking.common.workerqueue.Worker
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification

internal class DisconnectWorker(
    private val ably: Ably,
    private val trackableId: String,
    private val callbackFunction: () -> Unit
) : Worker<SubscriberProperties, WorkerSpecification> {

    override fun doWork(
        properties: SubscriberProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): SubscriberProperties {
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
