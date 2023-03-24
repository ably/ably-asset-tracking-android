package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.common.workerqueue.DefaultWorker
import com.ably.tracking.subscriber.SubscriberProperties
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification

internal class UpdateChannelConnectionStateWorker(
    private val channelConnectionStateChange: ConnectionStateChange,
    private val presenceHistory: List<PresenceMessage>?
) : DefaultWorker<SubscriberProperties, WorkerSpecification>() {
    override fun doWork(
        properties: SubscriberProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): SubscriberProperties {
        properties.updateForChannelConnectionStateChangeAndThenEmitStateEventsIfRequired(
            channelConnectionStateChange, presenceHistory
        )
        return properties
    }
}
