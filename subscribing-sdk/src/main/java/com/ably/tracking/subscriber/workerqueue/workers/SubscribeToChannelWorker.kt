package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.subscriber.SubscriberInteractor
import com.ably.tracking.subscriber.SubscriberProperties
import com.ably.tracking.common.workerqueue.CallbackWorker
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification

internal class SubscribeToChannelWorker(
    private val subscriberInteractor: SubscriberInteractor,
    callbackFunction: ResultCallbackFunction<Unit>
) : CallbackWorker<SubscriberProperties, WorkerSpecification>(callbackFunction) {
    override fun doWork(
        properties: SubscriberProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): SubscriberProperties {
        subscriberInteractor.subscribeForChannelState()
        subscriberInteractor.subscribeForEnhancedEvents(properties.presenceData)
        subscriberInteractor.subscribeForRawEvents(properties.presenceData)
        callbackFunction(Result.success(Unit))
        return properties
    }
}
