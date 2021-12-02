package com.ably.tracking.publisher.workerqueue.resulthandlers

import com.ably.tracking.publisher.AddTrackableFailedEvent
import com.ably.tracking.publisher.ConnectionForTrackableCreatedEvent
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.workerqueue.AddTrackableWorkResult
import com.ably.tracking.publisher.workerqueue.WorkResult
import com.ably.tracking.publisher.workerqueue.WorkResultHandler
import com.ably.tracking.publisher.workerqueue.workers.Worker

internal class AddTrackableResultHandler : WorkResultHandler {
    override fun handle(
        workResult: WorkResult,
        corePublisher: CorePublisher
    ): Worker? {
        when (workResult) {
            is AddTrackableWorkResult.AlreadyIn -> workResult.callbackFunction(
                Result.success(workResult.trackableStateFlow)
            )

            is AddTrackableWorkResult.Fail -> corePublisher.request(
                AddTrackableFailedEvent(
                    workResult.trackable,
                    workResult.callbackFunction, workResult.exception as Exception
                )
            )
            is AddTrackableWorkResult.Success -> corePublisher.request(
                ConnectionForTrackableCreatedEvent(
                    workResult.trackable,
                    workResult.callbackFunction
                )
            )
        }
        return null
    }
}
