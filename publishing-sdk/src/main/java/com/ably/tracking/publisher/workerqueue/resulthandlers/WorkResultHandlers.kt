package com.ably.tracking.publisher.workerqueue.resulthandlers

import com.ably.tracking.publisher.workerqueue.AddTrackableWorkResult
import com.ably.tracking.publisher.workerqueue.WorkResult
import com.ably.tracking.publisher.workerqueue.WorkResultHandler

internal fun getWorkResultHandler(workResult: WorkResult): WorkResultHandler {
    when (workResult) {
        is AddTrackableWorkResult.Success -> AddTrackableResultHandler()
        is AddTrackableWorkResult.Fail -> AddTrackableResultHandler()
        is AddTrackableWorkResult.AlreadyIn -> AddTrackableResultHandler()
    }
    return AddTrackableResultHandler()
}
