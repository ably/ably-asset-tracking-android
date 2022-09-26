package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.subscriber.SubscriberStateManipulator
import com.ably.tracking.subscriber.Properties
import com.ably.tracking.subscriber.workerqueue.CallbackWorker
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification

internal class SubscribeToChannelWorker(
    private val subscriberStateManipulator: SubscriberStateManipulator,
    callbackFunction: ResultCallbackFunction<Unit>
) : CallbackWorker(callbackFunction) {
    override fun doWork(
        properties: Properties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ) {
        subscriberStateManipulator.subscribeForChannelState()
        subscriberStateManipulator.subscribeForEnhancedEvents(properties.presenceData)
        subscriberStateManipulator.subscribeForRawEvents(properties.presenceData)
        callbackFunction(Result.success(Unit))
    }
}
