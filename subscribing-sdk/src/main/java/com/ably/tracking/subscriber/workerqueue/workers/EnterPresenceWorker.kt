package com.ably.tracking.subscriber.workerqueue.workers

import com.ably.tracking.ConnectionException
import com.ably.tracking.common.Ably
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.isFatal
import com.ably.tracking.common.workerqueue.DefaultWorker
import com.ably.tracking.subscriber.SubscriberProperties
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification
import kotlinx.coroutines.delay

/**
 * How long should we wait before queueing enter retry presence work if enter presence fails.
 */
private const val PRESENCE_ENTER_DELAY_IN_MILLISECONDS = 15_000L

internal class EnterPresenceWorker(
    private val trackableId: String,
    private val ably: Ably
) : DefaultWorker<SubscriberProperties, WorkerSpecification>() {

    override fun doWork(
        properties: SubscriberProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): SubscriberProperties {
        doAsyncWork {
            enterPresence(postWork, properties.presenceData)
        }

        return properties
    }

    private suspend fun enterPresence(
        postWork: (WorkerSpecification) -> Unit,
        presenceData: PresenceData
    ) {
        val waitForChannelToAttachResult = ably.waitForChannelToAttach(trackableId)
        if (waitForChannelToAttachResult.isFailure) {
            postDisconnectWork(postWork)
            return
        }

        val enterPresenceResult = ably.enterChannelPresence(
            trackableId = trackableId,
            presenceData = presenceData
        )

        val enterPresenceThrowable = enterPresenceResult.exceptionOrNull()
        if (enterPresenceThrowable != null) {
            if (isFatalFailure(enterPresenceThrowable)) {
                postDisconnectWork(postWork)
            } else {
                delay(PRESENCE_ENTER_DELAY_IN_MILLISECONDS)
                postWork(WorkerSpecification.EnterPresence(trackableId))
            }
        }
    }

    private fun isFatalFailure(throwable: Throwable): Boolean =
        throwable is ConnectionException && throwable.isFatal() && throwable.errorInformation.code != Ably.ENTER_PRESENCE_ON_SUSPENDED_CHANNEL_ERROR_CODE

    private fun postDisconnectWork(postWork: (WorkerSpecification) -> Unit) {
        postWork(
            WorkerSpecification.Disconnect(trackableId) {}
        )
    }

    override fun onUnexpectedAsyncError(
        exception: Exception,
        postWork: (WorkerSpecification) -> Unit
    ) {
        postWork(WorkerSpecification.EnterPresence(trackableId))
    }
}
