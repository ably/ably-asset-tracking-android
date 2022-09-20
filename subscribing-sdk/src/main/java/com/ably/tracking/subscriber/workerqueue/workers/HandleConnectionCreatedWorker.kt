package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.subscriber.CoreSubscriber
import com.ably.tracking.subscriber.Properties
import com.ably.tracking.subscriber.workerqueue.CallbackWorker
import com.ably.tracking.subscriber.workerqueue.WorkerParams

internal class HandleConnectionCreatedWorker(
    private val ably: Ably,
    private val trackableId: String,
    callbackFunction: ResultCallbackFunction<Unit>
) : CallbackWorker(callbackFunction) {
    override fun doWork(
        properties: Properties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerParams) -> Unit
    ) {
        ably.subscribeForPresenceMessages(
            trackableId = trackableId,
            listener = { postWork(WorkerParams.HandlePresenceMessage(it)) },
            callback = { subscribeResult ->
                if (subscribeResult.isSuccess) {
                    postWork(WorkerParams.HandleConnectionReady(callbackFunction))
                } else {
                    ably.disconnect(trackableId, properties.presenceData) {
                        callbackFunction(subscribeResult)
                    }
                }
            }
        )
    }
}
