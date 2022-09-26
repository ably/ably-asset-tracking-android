package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.subscriber.Properties
import com.ably.tracking.subscriber.SubscriberStateManipulator
import com.ably.tracking.subscriber.workerqueue.CallbackWorker
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification

internal class StartConnectionWorker(
    private val ably: Ably,
    private val subscriberStateManipulator: SubscriberStateManipulator,
    private val trackableId: String,
    callbackFunction: ResultCallbackFunction<Unit>
) : CallbackWorker(callbackFunction) {
    override fun doWork(
        properties: Properties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ) {
        doAsyncWork {
            subscriberStateManipulator.updateTrackableState(properties)
            val result = ably.connect(
                trackableId,
                properties.presenceData,
                useRewind = true,
                willSubscribe = true
            )

            if (result.isSuccess) {
                postWork(WorkerSpecification.SubscribeForPresenceMessages(callbackFunction))
            } else {
                callbackFunction(result)
            }
        }
    }
}
