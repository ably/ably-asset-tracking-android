package com.ably.tracking.publisher.workerqueue.resulthandlers

import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.workerqueue.results.StopResult
import com.ably.tracking.publisher.workerqueue.results.WorkResult
import com.ably.tracking.publisher.workerqueue.workers.Worker

internal class StopResultHandler : WorkResultHandler {
    override fun handle(workResult: WorkResult, corePublisher: CorePublisher): Worker? {
        when (workResult) {
            is StopResult.Success -> workResult.callbackFunction(Result.success(Unit))
            is StopResult.Fail -> workResult.callbackFunction(Result.failure(workResult.exception))
        }
        return null
    }
}
