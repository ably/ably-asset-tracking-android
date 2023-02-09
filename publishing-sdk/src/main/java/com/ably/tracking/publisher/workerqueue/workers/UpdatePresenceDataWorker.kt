package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.common.PresenceData
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
            val waitResult = ably.waitForChannelToAttach(trackableId)
            if (waitResult.isSuccess) {
                // For now we ignore the result of this operation but perhaps we should retry it if it fails
                ably.updatePresenceData(trackableId, presenceData)
            }
        }
        return properties
    }
}
