package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.ConnectionException
import com.ably.tracking.common.Ably
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.isFatal
import com.ably.tracking.common.workerqueue.DefaultWorker
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification
import kotlinx.coroutines.delay

/**
 * How long should we wait before queueing enter retry presence work if enter presence fails.
 */
private const val PRESENCE_ENTER_DELAY_IN_MILLISECONDS = 15_000L

internal class EnterPresenceWorker(
    private val trackable: Trackable,
    private val ably: Ably
) : DefaultWorker<PublisherProperties, WorkerSpecification>() {

    override fun doWork(
        properties: PublisherProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): PublisherProperties {
        if (
            properties.trackables.contains(trackable) &&
            !properties.trackableRemovalGuard.isMarkedForRemoval(trackable)
        ) {
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
        val waitForChannelToAttachResult = ably.waitForChannelToAttach(trackable.id)
        if (waitForChannelToAttachResult.isFailure) {
            postFailTrackableWork(
                postWork,
                waitForChannelToAttachResult
            )
            return
        }

        val enterPresenceResult = ably.enterChannelPresence(
            trackableId = trackable.id,
            presenceData = presenceData
        )

        when {
            enterPresenceResult.isSuccess -> postWork(
                WorkerSpecification.EnterPresenceSuccess(
                    trackable
                )
            )
            isFatalFailure(enterPresenceResult) -> postFailTrackableWork(
                postWork,
                enterPresenceResult
            )
            else -> {
                delay(PRESENCE_ENTER_DELAY_IN_MILLISECONDS)
                postWork(WorkerSpecification.EnterPresence(trackable))
            }
        }
    }

    private fun isFatalFailure(result: Result<Unit>): Boolean {
        val connectionException = result.exceptionOrNull() as? ConnectionException
        return connectionException != null && connectionException.isFatal() &&
            connectionException.errorInformation.code != Ably.ENTER_PRESENCE_ON_SUSPENDED_CHANNEL_ERROR_CODE
    }

    private fun postFailTrackableWork(
        postWork: (WorkerSpecification) -> Unit,
        result: Result<Unit>
    ) {
        val errorInformation = (result.exceptionOrNull() as ConnectionException).errorInformation
        postWork(WorkerSpecification.FailTrackable(trackable, errorInformation))
    }

    override fun onUnexpectedAsyncError(
        exception: Exception,
        postWork: (WorkerSpecification) -> Unit
    ) {
        postWork(WorkerSpecification.EnterPresence(trackable))
    }
}
