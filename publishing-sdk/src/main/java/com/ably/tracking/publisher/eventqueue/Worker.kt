package com.ably.tracking.publisher.eventqueue

import kotlinx.coroutines.delay

/**
 * This interface is for workers whose sole purpose is to process works that were enqueued in [WorkerQueue]
* */
interface Worker {
    /**
     * This function is provided in order for implementors  to implement synchronous work (such as state read and
     * manipulation)
     * @return an optional suspending function that returns a [WorkResult]. This suspending function is intended for
     * [WorkerQueue] to process asynchornous operations.
     * **/
    fun doWork(): (suspend () -> WorkResult)?
}
class AddTrackableWorker(val delay:Long): Worker {
    override fun doWork(): (suspend () -> WorkResult)? {
        //do some synchronous work here and return async part
        println("Doing sync part of AddTrackableWorker")
        return {
            println("Doing async part of AddTrackableWorker")
            delay(delay)
            AddTrackableResult(delay)
        }
    }
}


