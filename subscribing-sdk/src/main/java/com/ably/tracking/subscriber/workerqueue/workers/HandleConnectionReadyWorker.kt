package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.subscriber.CoreSubscriber
import com.ably.tracking.subscriber.Properties
import com.ably.tracking.subscriber.workerqueue.CallbackWorker
import com.ably.tracking.subscriber.workerqueue.WorkerParams

internal class HandleConnectionReadyWorker(
    private val coreSubscriber: CoreSubscriber,
    callbackFunction: ResultCallbackFunction<Unit>
) : CallbackWorker(callbackFunction) {
    override fun doWork(
        properties: Properties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerParams) -> Unit
    ) {
        coreSubscriber.subscribeForChannelState()
        coreSubscriber.subscribeForEnhancedEvents(properties.presenceData)
        coreSubscriber.subscribeForRawEvents(properties.presenceData)
        callbackFunction(Result.success(Unit))
    }
}
