package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.isFatalAblyFailure
import com.ably.tracking.common.workerqueue.DefaultWorker
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification
import kotlinx.coroutines.delay

/**
 * How long should we wait before re-queueing the work if starting or stopping Ably connection is in progress.
 * If after the delay the Ably connection process is still in progress the work will be re-queued again.
 */
private const val WORK_DELAY_IN_MILLISECONDS = 200L

internal class RetryEnterPresenceWorker(
    private val trackable: Trackable,
    private val ably: Ably
) : DefaultWorker<PublisherProperties, WorkerSpecification>() {

    override fun doWork(
        properties: PublisherProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): PublisherProperties {
        if (!properties.trackableRemovalGuard.isMarkedForRemoval(trackable)) {
            doAsyncWork {
                enterPresence(postWork, properties.presenceData)
            }
        }
        return properties
    }

    private suspend fun enterPresence(
        postWork: (WorkerSpecification) -> Unit,
        presenceData: PresenceData
    ) {
        val connectResult = ably.connect(
            trackableId = trackable.id,
            presenceData = presenceData,
            willPublish = true,
        )

        when {
            connectResult.isFatalAblyFailure() -> throw NotImplementedError() // TODO How to handle this case?
            connectResult.isFailure -> {
                delay(WORK_DELAY_IN_MILLISECONDS)
                postWork(WorkerSpecification.RetryEnterPresence(trackable))
            }
        }
    }

    override fun onUnexpectedAsyncError(
        exception: Exception,
        postWork: (WorkerSpecification) -> Unit
    ) {
        postWork(WorkerSpecification.RetryEnterPresence(trackable))
    }
}
