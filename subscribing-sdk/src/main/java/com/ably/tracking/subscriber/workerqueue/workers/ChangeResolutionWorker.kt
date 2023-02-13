package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.Resolution
import com.ably.tracking.common.Ably
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.isFatalAblyFailure
import com.ably.tracking.subscriber.SubscriberProperties
import com.ably.tracking.common.workerqueue.DefaultWorker
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification

internal class ChangeResolutionWorker(
    private val ably: Ably,
    private val trackableId: String,
    private val resolution: Resolution?
) : DefaultWorker<SubscriberProperties, WorkerSpecification>() {
    override fun doWork(
        properties: SubscriberProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): SubscriberProperties {
        properties.presenceData = properties.presenceData.copy(resolution = resolution)
        if (!properties.containsUpdatingResolution(trackableId, resolution)) {
            properties.addUpdatingResolution(trackableId, resolution)
        }
        if (properties.isLastUpdatingResolution(trackableId, resolution)) {
            doAsyncWork {
                val result = waitAndUpdatePresence(properties.presenceData)
                if (result.isFailure && !result.isFatalAblyFailure()) {
                    postWork(WorkerSpecification.ChangeResolution(resolution))
                } else {
                    postWork(WorkerSpecification.ChangeResolutionSuccess(resolution))
                }
            }
        } else {
            properties.removeUpdatingResolution(trackableId, resolution)
        }
        return properties
    }

    private suspend fun waitAndUpdatePresence(presenceData: PresenceData): Result<Unit> {
        val waitResult = ably.waitForChannelToAttach(trackableId)
        return if (waitResult.isSuccess) {
            ably.updatePresenceData(trackableId, presenceData)
        } else {
            waitResult
        }
    }
}
