package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.subscriber.Core
import com.ably.tracking.subscriber.Properties
import com.ably.tracking.subscriber.workerqueue.CallbackWorker
import com.ably.tracking.subscriber.workerqueue.WorkerParams

internal class StartConnectionWorker(
    private val ably: Ably,
    private val core: Core,
    private val trackableId: String,
    callbackFunction: ResultCallbackFunction<Unit>
) : CallbackWorker(callbackFunction) {
    override fun doWork(
        properties: Properties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerParams) -> Unit
    ) {
        core.updateTrackableState(properties)
        //TODO convert to coroutines using async
        ably.connect(
            trackableId,
            properties.presenceData,
            useRewind = true,
            willSubscribe = true
        ) {
            if (it.isSuccess) {
                postWork(WorkerParams.SubscribeForPresenceMessages(callbackFunction))
            } else {
                callbackFunction(it)
            }
        }
    }
}
