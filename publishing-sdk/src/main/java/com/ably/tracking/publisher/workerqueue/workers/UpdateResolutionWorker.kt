package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.Resolution
import com.ably.tracking.common.Ably
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.isFatalAblyFailure
import com.ably.tracking.common.workerqueue.DefaultWorker
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.workerqueue.WorkerSpecification

internal class UpdateResolutionWorker(
    private val trackableId: String,
    private val resolution: Resolution,
    private val ably: Ably,
) : DefaultWorker<PublisherProperties, WorkerSpecification>() {

    override fun doWork(
        properties: PublisherProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): PublisherProperties {
        if (!properties.containsUpdatingResolution(trackableId, resolution)) {
            properties.addUpdatingResolution(trackableId, resolution)
        }

        if (!properties.isLastUpdatingResolution(trackableId, resolution)) {
            properties.removeUpdatingResolution(trackableId, resolution)
            return properties
        }

        waitAndUpdatePresenceAsync(doAsyncWork, properties, postWork)
        return properties
    }

    private fun waitAndUpdatePresenceAsync(
        doAsyncWork: (suspend () -> Unit) -> Unit,
        properties: PublisherProperties,
        postWork: (WorkerSpecification) -> Unit
    ) {
        doAsyncWork {
            val result = waitAndUpdatePresence(properties.presenceData)
            if (result.isFailure && !result.isFatalAblyFailure()) {
                postWork(WorkerSpecification.UpdateResolution(trackableId, resolution))
            } else {
                postWork(WorkerSpecification.UpdateResolutionSuccess(trackableId, resolution))
            }
        }
    }

    private suspend fun waitAndUpdatePresence(presenceData: PresenceData): Result<Unit> {
        val waitResult = ably.waitForChannelToAttach(trackableId)
        val updatedPresenceData = presenceData.copy(resolution = resolution)
        return if (waitResult.isSuccess) {
            ably.updatePresenceData(trackableId, updatedPresenceData)
        } else {
            waitResult
        }
    }
}
