package com.ably.tracking.publisher.eventqueue

import android.util.Log
import com.ably.tracking.publisher.AddTrackableFailedEvent
import com.ably.tracking.publisher.ConnectionForTrackableCreatedEvent
import com.ably.tracking.publisher.CorePublisher

private const val TAG = "ResultHandler"
/*interface WorkResultHandler<in T:WorkResult>{
    fun handle(workResult: T)
}*/

internal class WorkResultHandler<in T:WorkResult>{
    fun handle(workResult: T,corePublisher: CorePublisher){
        if (workResult is AddTrackableResult){
            handleAddTrackableResult(workResult as AddTrackableResult,corePublisher)
        }
    }

    private fun handleAddTrackableResult(addTrackableResult: AddTrackableResult,corePublisher: CorePublisher) {
        when(addTrackableResult){
            is AddTrackableResult.Success -> ConnectionForTrackableCreatedEvent(addTrackableResult.trackable,
                addTrackableResult
                .handler)
            is AddTrackableResult.Fail -> corePublisher.request(AddTrackableFailedEvent(addTrackableResult
                .trackable, addTrackableResult.handler, addTrackableResult.exception as Exception
            ))
        }
    }
}

