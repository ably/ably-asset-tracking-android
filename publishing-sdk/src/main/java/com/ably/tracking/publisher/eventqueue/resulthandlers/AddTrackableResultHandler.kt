package com.ably.tracking.publisher.eventqueue.resulthandlers

import com.ably.tracking.publisher.AddTrackableFailedEvent
import com.ably.tracking.publisher.ConnectionForTrackableCreatedEvent
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.eventqueue.AddTrackableWorkResult
import com.ably.tracking.publisher.eventqueue.WorkResult
import com.ably.tracking.publisher.eventqueue.WorkResultHandler
import com.ably.tracking.publisher.eventqueue.WorkResultHandlerResult

private const val TAG = "AddTrackableResultHandl"

internal class AddTrackableResultHandler : WorkResultHandler {
    override fun handle(
        workResult: WorkResult,
        corePublisher: CorePublisher
    ): WorkResultHandlerResult? {
        when (workResult) {
            is AddTrackableWorkResult.AlreadyIn -> workResult.callbackFunction.invoke(
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

        /*        return when (workResult) {
            is AddTrackableResult.Success -> WorkResultHandlerResult(
                ConnectionForTrackableCreatedWorker
                    (workResult.trackable),
                resultCallbackFunctions
            )
            is AddTrackableResult.Fail -> WorkResultHandlerResult(
                AddTrackableFailedWorker(workResult.exception),
                resultCallbackFunctions
            )
            is AddTrackableResult.AlreadyIn -> TODO()
        }*/
    }

}
