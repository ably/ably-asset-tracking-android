package com.ably.tracking.publisher.workerqueue.resulthandlers

import com.ably.tracking.publisher.ConnectionForTrackableCreatedEvent
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.workerqueue.results.AddTrackableWorkResult
import com.ably.tracking.publisher.workerqueue.workers.AddTrackableFailedWorker
import com.ably.tracking.publisher.workerqueue.workers.SyncAsyncWorker

internal class AddTrackableResultHandler : WorkResultHandler<AddTrackableWorkResult> {
    override fun handle(
        workResult: AddTrackableWorkResult,
        corePublisher: CorePublisher
    ): SyncAsyncWorker? {
        when (workResult) {
            is AddTrackableWorkResult.AlreadyIn -> workResult.callbackFunction(
                Result.success(workResult.trackableStateFlow)
            )

            is AddTrackableWorkResult.Fail ->
                return AddTrackableFailedWorker(
                    workResult.trackable, workResult.callbackFunction, workResult.exception as Exception
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
