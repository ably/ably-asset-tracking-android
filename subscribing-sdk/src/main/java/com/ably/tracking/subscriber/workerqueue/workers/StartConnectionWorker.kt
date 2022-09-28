package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.subscriber.Properties
import com.ably.tracking.subscriber.SubscriberInteractor
import com.ably.tracking.subscriber.workerqueue.CallbackWorker
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification

internal class StartConnectionWorker(
    private val ably: Ably,
    private val subscriberInteractor: SubscriberInteractor,
    private val trackableId: String,
    callbackFunction: ResultCallbackFunction<Unit>
) : CallbackWorker(callbackFunction) {
    override fun doWork(
        properties: Properties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ) {
        subscriberInteractor.updateTrackableState(properties)
        val presenceData = properties.presenceData.copy()
        doAsyncWork {
            val result = ably.connect(
                trackableId,
                presenceData,
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
