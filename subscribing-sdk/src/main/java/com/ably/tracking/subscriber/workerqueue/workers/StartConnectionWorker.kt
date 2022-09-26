package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.subscriber.Properties
import com.ably.tracking.subscriber.SubscriberStateManipulator
import com.ably.tracking.subscriber.workerqueue.CallbackWorker
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification

internal class StartConnectionWorker(
    private val ably: Ably,
    private val subscriberStateManipulator: SubscriberStateManipulator,
    private val trackableId: String,
    callbackFunction: ResultCallbackFunction<Unit>
) : CallbackWorker(callbackFunction) {
    override fun doWork(
        properties: Properties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ) {
        doAsyncWork{
            //TODO clean try..catch?
            try {
                subscriberStateManipulator.updateTrackableState(properties)
                ably.connect(
                    trackableId,
                    properties.presenceData,
                    useRewind = true,
                    willSubscribe = true
                )
                postWork(WorkerSpecification.SubscribeForPresenceMessages(callbackFunction))
            } catch (exception: Exception) {
                callbackFunction(Result.failure(exception))
            }
        }
    }
}
