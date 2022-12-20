package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.common.workerqueue.CallbackWorker
import com.ably.tracking.subscriber.SubscriberProperties
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification

internal class ProcessInitialPresenceMessagesWorker(
    private val presenceMessages: List<PresenceMessage>,
    callbackFunction: ResultCallbackFunction<Unit>,
) : CallbackWorker<SubscriberProperties, WorkerSpecification>(callbackFunction) {
    override fun doWork(
        properties: SubscriberProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): SubscriberProperties {
        presenceMessages.forEach { presenceMessage ->
            properties.updateForPresenceMessage(presenceMessage)
        }
        properties.emitStateEventsIfRequired()
        postWork(WorkerSpecification.SubscribeToChannel(callbackFunction))
        return properties
    }
}
