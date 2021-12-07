package com.ably.tracking.publisher.workerqueue.resulthandlers

import com.ably.tracking.publisher.CorePublisher
import com.ably.tracking.publisher.workerqueue.results.WorkResult
import com.ably.tracking.publisher.workerqueue.workers.Worker

/**
 * This interface is intended for handling [WorkResult]s that are received from  [Worker]s synchrnous or asynchronus
 * work.
 *
 * Design note: A more generic interface like WorkResultHandler<T> could have been a nicer choice as implmentors
 * could then expose a narrower typing of [WorkResult]. However I have not found a possible way to represent concrete
 * handlers as generic interface without unchecked casts.
 * **/
internal interface WorkResultHandler<in T : WorkResult> {
    /**
     * Handles [WorkResult] given to it. Impelementors are responsible for what type of work results they process.
     *
     * @param workResult : [WorkResult] to be handled
     * @param corePublisher: This is a temporary reference of [CorePublisher] that is kept here to maintain
     * compatibility with refactored code.
     *
     * Implementors must delegate work to [corePublisher]  if the required [Worker]s have not been implemented yet.
     *
     * @return Worker? if implementors decide there is a need to add another worker to the queue.
     * **/
    fun handle(workResult: T, corePublisher: CorePublisher): Worker?
}
