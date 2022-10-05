package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.subscriber.SubscriberInteractor
import com.ably.tracking.subscriber.Properties
import com.ably.tracking.subscriber.workerqueue.CallbackWorker
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification

internal class SubscribeToChannelWorker(
    private val subscriberInteractor: SubscriberInteractor,
    callbackFunction: ResultCallbackFunction<Unit>
) : CallbackWorker(callbackFunction) {
    override fun doWork(
        properties: Properties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): Properties {
        subscriberInteractor.subscribeForChannelState()
        subscriberInteractor.subscribeForEnhancedEvents(properties.presenceData)
        subscriberInteractor.subscribeForRawEvents(properties.presenceData)
        callbackFunction(Result.success(Unit))
        return properties
    }
}
