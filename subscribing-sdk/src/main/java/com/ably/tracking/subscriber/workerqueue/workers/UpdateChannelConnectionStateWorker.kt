package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.subscriber.Core
import com.ably.tracking.subscriber.CoreSubscriber
import com.ably.tracking.subscriber.Properties
import com.ably.tracking.subscriber.workerqueue.Worker
import com.ably.tracking.subscriber.workerqueue.WorkerParams

internal class UpdateChannelConnectionStateWorker(
    private val channelConnectionStateChange: ConnectionStateChange,
    private val core: Core
) : Worker {
    override fun doWork(
        properties: Properties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerParams) -> Unit
    ) {
        properties.lastChannelConnectionStateChange = channelConnectionStateChange
        core.updateTrackableState(properties)
    }

    override fun doWhenStopped(exception: Exception) = Unit
}
