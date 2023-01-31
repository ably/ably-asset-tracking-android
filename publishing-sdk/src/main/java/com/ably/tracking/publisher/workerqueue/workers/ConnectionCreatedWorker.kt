package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.ConnectionException
import com.ably.tracking.ErrorInformation
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.common.logging.w
import com.ably.tracking.common.workerqueue.Worker
import com.ably.tracking.logging.LogHandler
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.PublisherState
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.WorkerSpecification
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume

private const val SUBSCRIBE_TO_PRESENCE_TIMEOUT = 5000L

internal class ConnectionCreatedWorker(
    private val trackable: Trackable,
    private val enteredPresence: Boolean,
    private val ably: Ably,
    private val logHandler: LogHandler?,
    private val presenceUpdateListener: ((presenceMessage: PresenceMessage) -> Unit),
    private val channelStateChangeListener: ((connectionStateChange: ConnectionStateChange) -> Unit),
) : Worker<PublisherProperties, WorkerSpecification> {
    /**
     * Whether the trackable is being removed.
     * Used to properly handle unexpected exceptions in [onUnexpectedAsyncError].
     */
    private var isBeingRemoved = false

    override fun doWork(
        properties: PublisherProperties,
        doAsyncWork: (suspend () -> Unit) -> Unit,
        postWork: (WorkerSpecification) -> Unit
    ): PublisherProperties {
        if (properties.state == PublisherState.CONNECTING) {
            // If we've made up this far it means the [AddTrackableWorker] succeeded and there's a working Ably connection
            properties.state = PublisherState.CONNECTED
        }
        if (properties.trackableRemovalGuard.isMarkedForRemoval(trackable)) {
            // Leave Ably channel.
            doAsyncWork {
                isBeingRemoved = true
                ably.disconnect(trackable.id, properties.presenceData)
                postWork(WorkerSpecification.TrackableRemovalRequested(trackable, Result.success(Unit)))
            }
            return properties
        }

        if (enteredPresence) {
            properties.trackableEnteredPresenceFlags[trackable.id] = true
        }

        doAsyncWork {
            try {
                val subscribeToPresenceResult = subscribeToPresenceMessages()
                subscribeToPresenceResult.getOrThrow()
                postWork(
                    createConnectionReadyWorkerSpecification(
                        isSubscribedToPresence = true
                    )
                )
            } catch (exception: ConnectionException) {
                logHandler?.w("Failed to subscribe to presence for trackable ${trackable.id}", exception)
                postWork(
                    createConnectionReadyWorkerSpecification(
                        isSubscribedToPresence = false
                    )
                )
            } catch (exception: TimeoutCancellationException) {
                logHandler?.w("Timeout subscribing to presence for trackable ${trackable.id}")
                postWork(
                    createConnectionReadyWorkerSpecification(
                        isSubscribedToPresence = false
                    )
                )
            }
        }

        return properties
    }

    private suspend fun subscribeToPresenceMessages(): Result<Unit> {
        return withTimeout(SUBSCRIBE_TO_PRESENCE_TIMEOUT) {
            suspendCancellableCoroutine { continuation ->
                ably.subscribeForPresenceMessages(trackable.id, presenceUpdateListener) { result ->
                    continuation.resume(result)
                }
            }
        }
    }

    private fun createConnectionReadyWorkerSpecification(isSubscribedToPresence: Boolean) =
        WorkerSpecification.ConnectionReady(
            trackable,
            channelStateChangeListener,
            presenceUpdateListener,
            isSubscribedToPresence = isSubscribedToPresence,
        )

    override fun doWhenStopped(exception: Exception) {
        // No op
    }

    /**
     * If something goes really wrong, fail the trackable.
     */
    override fun onUnexpectedError(exception: Exception, postWork: (WorkerSpecification) -> Unit) {
        postWork(
            WorkerSpecification.FailTrackable(
                trackable,
                ErrorInformation("Unexpected error on connection created: $exception")
            )
        )
    }

    override fun onUnexpectedAsyncError(exception: Exception, postWork: (WorkerSpecification) -> Unit) {
        if (isBeingRemoved) {
            postWork(
                WorkerSpecification.TrackableRemovalRequested(trackable, Result.failure(exception))
            )
        } else {
            // If the async work fails we carry on as if it failed with a regular exception
            postWork(createConnectionReadyWorkerSpecification(isSubscribedToPresence = false))
        }
    }
}
