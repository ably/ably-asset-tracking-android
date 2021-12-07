package com.ably.tracking.publisher.workerqueue.resulthandlers

import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.RemoveTrackableRequestedException
import com.ably.tracking.publisher.workerqueue.results.TrackableRemovalRequestedResult
import com.ably.tracking.publisher.workerqueue.results.WorkResult
import com.ably.tracking.publisher.workerqueue.workers.Worker

internal class TrackableRemovalRequestedResultHandler : WorkResultHandler {
    override fun handle(workResult: WorkResult, corePublisher: CorePublisher): Worker? {
        when (workResult) {
            is TrackableRemovalRequestedResult.Fail -> workResult.callbackFunction(
                Result.failure(RemoveTrackableRequestedException())
            )
            is TrackableRemovalRequestedResult.Success -> TODO() // implementation not needed, left here to
            // explicitly set the case
        }
        return null
    }
}
