package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.ConnectionException
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ConnectionState
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.isFatalAblyFailure
import com.ably.tracking.common.workerqueue.DefaultWorker
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification
import kotlinx.coroutines.delay
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * How long should we wait before queueing enter retry presence work if enter presence fails.
 */
private const val PRESENCE_ENTER_DELAY_IN_MILLISECONDS = 15_000L

internal class RetryEnterPresenceWorker(
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
        val waitForChannelToBeConnectedResult = waitForChannelToBeConnected(trackable)
        if (waitForChannelToBeConnectedResult.isFailure) {
            postFailTrackableWork(
                postWork,
                waitForChannelToBeConnectedResult
            )
            return
        }

        val enterPresenceResult = ably.enterChannelPresence(
            trackableId = trackable.id,
            presenceData = presenceData
        )

        when {
            enterPresenceResult.isSuccess -> postWork(
                WorkerSpecification.RetryEnterPresenceSuccess(
                    trackable
                )
            )
            enterPresenceResult.isFatalAblyFailure() -> postFailTrackableWork(
                postWork,
                enterPresenceResult
            )
            else -> {
                delay(PRESENCE_ENTER_DELAY_IN_MILLISECONDS)
                postWork(WorkerSpecification.RetryEnterPresence(trackable))
            }
        }
    }

    private suspend fun waitForChannelToBeConnected(trackable: Trackable): Result<Unit> {
        return suspendCoroutine { continuation ->

            var resumed = false
            ably.subscribeForChannelStateChange(trackable.id) {
                if (resumed) {
                    return@subscribeForChannelStateChange
                }

                if (it.state == ConnectionState.ONLINE) {
                    resumed = true
                    continuation.resume(Result.success(Unit))
                }
                if (it.state == ConnectionState.FAILED) {
                    resumed = true
                    continuation.resume(Result.failure(ConnectionException(it.errorInformation!!)))
                }
            }
        }
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
        postWork(WorkerSpecification.RetryEnterPresence(trackable))
    }
}
