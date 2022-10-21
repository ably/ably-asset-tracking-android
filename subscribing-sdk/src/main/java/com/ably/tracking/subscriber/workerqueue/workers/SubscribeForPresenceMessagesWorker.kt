package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.subscriber.SubscriberProperties
import com.ably.tracking.common.workerqueue.CallbackWorker
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification

internal class SubscribeForPresenceMessagesWorker(
    private val ably: Ably,
    private val trackableId: String,
    callbackFunction: ResultCallbackFunction<Unit>
) : CallbackWorker<SubscriberProperties, WorkerSpecification>(callbackFunction) {
    override fun doWork(
        properties: SubscriberProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): SubscriberProperties {
        doAsyncWork {
            val result = ably.subscribeForPresenceMessages(
                trackableId = trackableId,
                listener = { postWork(WorkerSpecification.UpdatePublisherPresence(it)) }
            )

            if (result.isSuccess) {
                postWork(WorkerSpecification.SubscribeToChannel(callbackFunction))
            } else {
                postWork(
                    WorkerSpecification.Disconnect(trackableId) {
                        callbackFunction(result)
                    }
                )
            }
        }
        return properties
    }
}
