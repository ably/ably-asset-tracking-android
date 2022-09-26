package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.Resolution
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.subscriber.Properties
import com.ably.tracking.subscriber.workerqueue.CallbackWorker
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification

internal class ChangeResolutionWorker(
    private val ably: Ably,
    private val trackableId: String,
    private val resolution: Resolution?,
    callbackFunction: ResultCallbackFunction<Unit>
) : CallbackWorker(callbackFunction) {
    override fun doWork(
        properties: Properties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ) {
        doAsyncWork {
            //TODO clean try..catch?
            try {
                properties.presenceData = properties.presenceData.copy(resolution = resolution)
                ably.updatePresenceData(trackableId, properties.presenceData)
                callbackFunction(Result.success(Unit))
            } catch (exception: Exception) {
                callbackFunction(Result.failure(exception))
            }
        }
    }
}
