package com.ably.tracking.publisher.workerqueue.resulthandlers

import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.workerqueue.results.SetActiveTrackableResult
import com.ably.tracking.publisher.workerqueue.results.WorkResult
import com.ably.tracking.publisher.workerqueue.workers.Worker

internal class SetActiveTrackableResultHandler : WorkResultHandler {
    override fun handle(workResult: WorkResult, corePublisher: CorePublisher): Worker? {
        if (workResult is SetActiveTrackableResult) {
            workResult.callbackFunction(Result.success(Unit))
        }
        return null
    }
}
