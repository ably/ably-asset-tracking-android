package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.ConnectionException
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
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * This worker subscribes to presence messages on the trackable channel.
 * Second of three steps required to add a trackable, previous step is [PrepareConnectionForTrackableWorker] and the next step is [AddTrackableToPublisherWorker].
 */
internal class SubscribeToTrackablePresenceWorker(
    private val trackable: Trackable,
    private val callbackFunction: AddTrackableCallbackFunction,
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
                val result = ably.disconnect(trackable.id, properties.presenceData)
                postWork(WorkerSpecification.TrackableRemovalRequested(trackable, callbackFunction, result))
            }
            return properties
        }

        doAsyncWork {
            val subscribeToPresenceResult = subscribeToPresenceMessages()
            try {
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
            }
        }

        return properties
    }

    private suspend fun subscribeToPresenceMessages(): Result<Unit> {
        return suspendCoroutine { continuation ->
            ably.subscribeForPresenceMessages(trackable.id, presenceUpdateListener) { result ->
                continuation.resume(result)
            }
        }
    }

    private fun createConnectionReadyWorkerSpecification(isSubscribedToPresence: Boolean) =
        WorkerSpecification.AddTrackableToPublisher(
            trackable,
            callbackFunction,
            channelStateChangeListener,
            presenceUpdateListener,
            isSubscribedToPresence = isSubscribedToPresence,
        )

    override fun doWhenStopped(exception: Exception) {
        callbackFunction(Result.failure(exception))
    }

    override fun onUnexpectedError(exception: Exception, postWork: (WorkerSpecification) -> Unit) {
        callbackFunction(Result.failure(exception))
    }

    override fun onUnexpectedAsyncError(exception: Exception, postWork: (WorkerSpecification) -> Unit) {
        if (isBeingRemoved) {
            postWork(
                WorkerSpecification.TrackableRemovalRequested(trackable, callbackFunction, Result.failure(exception))
            )
        } else {
            // If the async work fails we carry on as if it failed with a regular exception
            postWork(createConnectionReadyWorkerSpecification(isSubscribedToPresence = false))
        }
    }
}
