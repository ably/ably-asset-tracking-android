package com.ably.tracking.publisher.eventqueue.resulthandlers

import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.eventqueue.AddTrackableResult
import com.ably.tracking.publisher.eventqueue.WorkResultHandler
import com.ably.tracking.publisher.eventqueue.WorkResultHandlerResult
import com.ably.tracking.publisher.eventqueue.workers.AddTrackableFailedWorker
import com.ably.tracking.publisher.eventqueue.workers.ConnectionForTrackableCreatedWorker

internal class AddTrackableResultHandler : WorkResultHandler<AddTrackableResult> {
    override fun handle(
        workResult: AddTrackableResult,
        resultCallbackFunctions: List<ResultCallbackFunction<*>>?,
        corePublisher: CorePublisher
    ): WorkResultHandlerResult? {
        return when (workResult) {
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
        }
    }

}
