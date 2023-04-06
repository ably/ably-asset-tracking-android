package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.common.Ably
import com.ably.tracking.common.ConnectionState
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.workerqueue.DefaultWorker
import com.ably.tracking.subscriber.SubscriberProperties
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification

internal class FetchHistoryForChannelConnectionStateChangeWorker(
    private val trackableId: String,
    private val channelConnectionStateChange: ConnectionStateChange,
    private val ably: Ably
) : DefaultWorker<SubscriberProperties, WorkerSpecification>() {

    companion object {
        private const val PRESENCE_HISTORY_DURATION = 4 * 60 * 60 * 1000L
    }

    override fun doWork(
        properties: SubscriberProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): SubscriberProperties {
        if (channelConnectionStateChange.state == ConnectionState.ONLINE) {
            getPresenceHistory(doAsyncWork, postWork)
        } else {
            postWork(
                WorkerSpecification.UpdateChannelConnectionState(
                    channelConnectionStateChange,
                    null
                )
            )
        }
        return properties
    }

    private fun getPresenceHistory(
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ) = doAsyncWork {
        val presenceHistoryResult =
            ably.getRecentPresenceHistory(trackableId, PRESENCE_HISTORY_DURATION)
        postWork(
            WorkerSpecification.UpdateChannelConnectionState(
                channelConnectionStateChange,
                presenceHistoryResult.getOrNull()
            )
        )
    }
}
