package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.isFatalAblyFailure
import com.ably.tracking.common.workerqueue.DefaultWorker
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.workerqueue.WorkerSpecification

internal class UpdatePresenceDataWorker(
    private val trackableId: String,
    private val presenceData: PresenceData,
    private val ably: Ably,
) : DefaultWorker<PublisherProperties, WorkerSpecification>() {

    override fun doWork(
        properties: PublisherProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): PublisherProperties {
        doAsyncWork {
            val result = waitAndUpdatePresence()
            if (result.isFailure && !result.isFatalAblyFailure()) {
                postWork(WorkerSpecification.UpdatePresenceData(trackableId, presenceData))
            }
        }
        return properties
    }

    private suspend fun waitAndUpdatePresence(): Result<Unit> {
        val waitResult = ably.waitForChannelToAttach(trackableId)
        return if (waitResult.isSuccess) {
            ably.updatePresenceData(trackableId, presenceData)
        } else {
            waitResult
        }
    }
}
