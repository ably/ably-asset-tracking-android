package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.subscriber.Core
import com.ably.tracking.subscriber.Properties
import com.ably.tracking.subscriber.workerqueue.CallbackWorker
import com.ably.tracking.subscriber.workerqueue.WorkerParams

internal class SubscribeToChannelWorker(
    private val core: Core,
    callbackFunction: ResultCallbackFunction<Unit>
) : CallbackWorker(callbackFunction) {
    override fun doWork(
        properties: Properties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerParams) -> Unit
    ) {
        core.subscribeForChannelState()
        core.subscribeForEnhancedEvents(properties.presenceData)
        core.subscribeForRawEvents(properties.presenceData)
        callbackFunction(Result.success(Unit))
    }
}
