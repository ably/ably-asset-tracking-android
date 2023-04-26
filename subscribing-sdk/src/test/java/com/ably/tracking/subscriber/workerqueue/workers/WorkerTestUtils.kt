package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution
import com.ably.tracking.subscriber.SubscriberProperties
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun MutableList<suspend () -> Unit>.appendWork(): (suspend () -> Unit) -> Unit =
    { asyncWork ->
        add(asyncWork)
    }

suspend fun MutableList<suspend () -> Unit>.executeAll() {
    forEach { it.invoke() }
}

suspend fun MutableList<suspend () -> Unit>.launchAll(scope: CoroutineScope) {
    forEach {
        scope.launch { it.invoke() }
    }
}

internal fun MutableList<WorkerSpecification>.appendSpecification(): (WorkerSpecification) -> Unit =
    { workSpecification ->
        add(workSpecification)
    }

internal fun createSubscriberProperties() =
    SubscriberProperties(Resolution(Accuracy.BALANCED, 100, 100.0), mockk())
