package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.subscriber.CoreSubscriber
import com.ably.tracking.subscriber.Properties
import com.ably.tracking.subscriber.workerqueue.CallbackWorker
import com.ably.tracking.subscriber.workerqueue.WorkerParams

internal class StartWorker(
    private val ably: Ably,
    private val coreSubscriber: CoreSubscriber,
    private val trackableId: String,
    callbackFunction: ResultCallbackFunction<Unit>
) : CallbackWorker(callbackFunction) {
    override fun doWork(
        properties: Properties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerParams) -> Unit
    ) {
        coreSubscriber.updateTrackableState(properties)
        ably.connect(
            trackableId,
            properties.presenceData,
            useRewind = true,
            willSubscribe = true
        ) {
            if (it.isSuccess) {
                postWork(WorkerParams.HandleConnectionCreated(callbackFunction))
            } else {
                callbackFunction(it)
            }
        }
    }
}
