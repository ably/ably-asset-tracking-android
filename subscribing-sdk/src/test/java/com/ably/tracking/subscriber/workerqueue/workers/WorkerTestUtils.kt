package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.subscriber.workerqueue.WorkerSpecification

fun MutableList<suspend () -> Unit>.appendWork(): (suspend () -> Unit) -> Unit =
    { asyncWork ->
        add(asyncWork)
    }

suspend fun MutableList<suspend () -> Unit>.executeAll() {
    forEach { it.invoke() }
}

internal fun MutableList<WorkerSpecification>.appendSpecification(): (WorkerSpecification) -> Unit =
    { workSpecification ->
        add(workSpecification)
    }
