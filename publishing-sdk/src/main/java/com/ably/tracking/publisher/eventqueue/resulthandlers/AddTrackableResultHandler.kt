package com.ably.tracking.publisher.eventqueue.resulthandlers

import com.ably.tracking.TrackableState
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.AddTrackableFailedEvent
import com.ably.tracking.publisher.ConnectionForTrackableCreatedEvent
import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.eventqueue.AddTrackableWorkResult
import com.ably.tracking.publisher.eventqueue.WorkResult
import com.ably.tracking.publisher.eventqueue.WorkResultHandler
import com.ably.tracking.publisher.eventqueue.WorkResultHandlerResult
import kotlinx.coroutines.flow.StateFlow

private const val TAG = "AddTrackableResultHandl"

internal class AddTrackableResultHandler : WorkResultHandler {
    override fun handle(
        workResult: WorkResult,
        callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>?,
        corePublisher: CorePublisher
    ): WorkResultHandlerResult? {
        when (workResult) {
            is AddTrackableWorkResult.AlreadyIn ->  callbackFunction?.invoke(
                Result.success(workResult.trackableStateFlow))
            /*event.handler(Result.success(state.trackableStateFlows[event.trackable.id]!!))*/

            is AddTrackableWorkResult.Fail -> corePublisher.request(
                AddTrackableFailedEvent(
                    workResult.trackable,
                    callbackFunction!!, workResult.exception as Exception
                )
            )
            is AddTrackableWorkResult.Success -> corePublisher.request(
                ConnectionForTrackableCreatedEvent(
                    workResult.trackable,
                    callbackFunction!!
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
