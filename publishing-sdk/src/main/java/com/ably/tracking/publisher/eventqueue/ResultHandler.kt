package com.ably.tracking.publisher.eventqueue

interface ResultHandler {
    fun handle(workResult: WorkResult)
}

interface AddTrackableResultHandler: ResultHandler {
    override fun handle(workResult: WorkResult) {
        println("Handling add trackable ")
    }
}
