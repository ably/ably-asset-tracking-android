package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.subscriber.SubscriberProperties
import com.ably.tracking.subscriber.SubscriberInteractor
import com.ably.tracking.common.workerqueue.CallbackWorker
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification

internal class StartConnectionWorker(
    private val ably: Ably,
    private val subscriberInteractor: SubscriberInteractor,
    private val trackableId: String,
    callbackFunction: ResultCallbackFunction<Unit>
) : CallbackWorker<SubscriberProperties, WorkerSpecification>(callbackFunction) {
    override fun doWork(
        properties: SubscriberProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): SubscriberProperties {
        subscriberInteractor.updateTrackableState(properties)
        doAsyncWork {
            val startAblyConnectionResult = ably.startConnection()
            if (startAblyConnectionResult.isFailure) {
                callbackFunction(startAblyConnectionResult)
            } else {
                val result = ably.connect(
                    trackableId,
                    properties.presenceData,
                    useRewind = true,
                    willSubscribe = true
                )

                if (result.isSuccess) {
                    postWork(WorkerSpecification.SubscribeForPresenceMessages(callbackFunction))
                } else {
                    callbackFunction(result)
                }
            }
        }
        return properties
    }
}
