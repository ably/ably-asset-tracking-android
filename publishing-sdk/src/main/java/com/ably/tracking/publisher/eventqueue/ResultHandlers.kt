package com.ably.tracking.publisher.eventqueue

import com.ably.tracking.publisher.eventqueue.workers.AddTrackableWorker
import com.ably.tracking.publisher.eventqueue.workers.Worker

internal object ResultHandlers {
    fun  handlerFor(worker:Worker): WorkResultHandler<WorkResult>{
        if (worker is AddTrackableWorker){
           return WorkResultHandler()
        }
        return WorkResultHandler()
    }
}
