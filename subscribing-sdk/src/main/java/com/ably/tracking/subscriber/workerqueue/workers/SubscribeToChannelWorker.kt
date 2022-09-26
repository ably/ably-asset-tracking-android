package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.subscriber.SubscriberStateManipulator
import com.ably.tracking.subscriber.Properties
import com.ably.tracking.subscriber.workerqueue.CallbackWorker
import com.ably.tracking.subscriber.workerqueue.WorkerParams

internal class SubscribeToChannelWorker(
    private val subscriberStateManipulator: SubscriberStateManipulator,
    callbackFunction: ResultCallbackFunction<Unit>
) : CallbackWorker(callbackFunction) {
    override fun doWork(
        properties: Properties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerParams) -> Unit
    ) {
        subscriberStateManipulator.subscribeForChannelState()
        subscriberStateManipulator.subscribeForEnhancedEvents(properties.presenceData)
        subscriberStateManipulator.subscribeForRawEvents(properties.presenceData)
        callbackFunction(Result.success(Unit))
    }
}
