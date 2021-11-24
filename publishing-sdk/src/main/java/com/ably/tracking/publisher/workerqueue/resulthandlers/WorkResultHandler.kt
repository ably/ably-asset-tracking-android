package com.ably.tracking.publisher.workerqueue

import com.ably.tracking.TrackableState
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.workerqueue.resulthandlers.AddTrackableResultHandler
import com.ably.tracking.publisher.workerqueue.workers.Worker
import kotlinx.coroutines.flow.StateFlow

/**
 * This interface is intended for handling [WorkResult]s that are received from  [Worker]s synchrnous or asynchronus
 * work.
 * Design note: A more generic interface like WorkResultHandler<T> could have been a nicer choice as implmentors
 * could then expose a narrower typing of [WorkResult]. However I have not found a possible way to represent concrete
 * handlers as generic interface without unchecked casts.
 * **/
internal interface WorkResultHandler {
    /**
     * Handles [WorkResult] given to it. Impelementors are responsible for what type of work results they process.
     * @param [workResult] : [WorkResult] to be handled
     * @param [corePublisher] : This is a temporary reference of [CorePublisher] that is kept here to maintain
     * compatibility with refactored code. Implementors must delegate work to this if the required [Worker]s has not
     * been implemented yet.
     * **/
    fun handle(workResult: WorkResult, corePublisher: CorePublisher): WorkResultHandlerResult?
}


internal data class WorkResultHandlerResult(
    val worker: Worker, val resultCallbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>? =
        null
)


internal fun getWorkResultHandler(workResult: WorkResult): WorkResultHandler {
    when (workResult) {
        is AddTrackableWorkResult.Success -> AddTrackableResultHandler()
        is AddTrackableWorkResult.Fail -> AddTrackableResultHandler()
        is AddTrackableWorkResult.AlreadyIn -> AddTrackableResultHandler()
    }
    return AddTrackableResultHandler()
}

