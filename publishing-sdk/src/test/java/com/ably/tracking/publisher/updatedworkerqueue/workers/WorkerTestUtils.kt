package com.ably.tracking.publisher.updatedworkerqueue.workers

import com.ably.tracking.publisher.updatedworkerqueue.WorkerSpecification

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
