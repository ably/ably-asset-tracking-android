package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.subscriber.SubscriberProperties
import com.ably.tracking.common.workerqueue.CallbackWorker
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification

internal class StartConnectionWorker(
    private val ably: Ably,
    private val trackableId: String,
    callbackFunction: ResultCallbackFunction<Unit>
) : CallbackWorker<SubscriberProperties, WorkerSpecification>(callbackFunction) {
    override fun doWork(
        properties: SubscriberProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): SubscriberProperties {
        properties.emitStateEventsIfRequired()
        doAsyncWork { connectAndEnterPresence(postWork) }
        return properties
    }

    private suspend fun connectAndEnterPresence(
        postWork: (WorkerSpecification) -> Unit
    ) {
        val startAblyConnectionResult = ably.startConnection()
        if (startAblyConnectionResult.isFailure) {
            callbackFunction(startAblyConnectionResult)
            return
        }

        val connectResult = ably.connect(
            trackableId,
            useRewind = true,
            willSubscribe = true
        )
        if (connectResult.isFailure) {
            callbackFunction(connectResult)
            return
        }

        postWork(WorkerSpecification.SubscribeForPresenceMessages(callbackFunction))
        postWork(WorkerSpecification.EnterPresence(trackableId))
    }
}
