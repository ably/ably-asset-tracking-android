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
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

/**
 * This worker subscribes to presence messages on the trackable channel.
 * Second of three steps required to add a trackable, previous step is [PrepareConnectionForTrackableWorker] and the next step is [FinishAddingTrackableToPublisherWorker].
 */
internal class SubscribeToTrackablePresenceMessagesWorker(
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

    companion object {
        /**
         * This timeout is added to prevent blocking the caller for a prolonged time and retrying later if subscribing does not complete smoothly.
         * The exact value was chosen arbitrarily.
         */
        private const val SUBSCRIBE_TO_PRESENCE_TIMEOUT = 2_000L
    }

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
                postWork(
                    WorkerSpecification.TrackableRemovalRequested(
                        trackable,
                        callbackFunction,
                        result
                    )
                )
            }
            return properties
        }

        doAsyncWork {
            try {
                withTimeout(SUBSCRIBE_TO_PRESENCE_TIMEOUT) {
                    val subscribeToPresenceResult =
                        ably.subscribeForPresenceMessages(trackable.id, presenceUpdateListener)
                    subscribeToPresenceResult.getOrThrow()
                }
                postWork(
                    createFinishAddingTrackableToPublisherWorkerSpecification(
                        isSubscribedToPresence = true
                    )
                )
            } catch (timeoutCancellationException: TimeoutCancellationException) {
                logHandler?.w(
                    "Subscribing to presence for trackable ${trackable.id} timed out",
                    timeoutCancellationException
                )
                postWork(
                    createFinishAddingTrackableToPublisherWorkerSpecification(
                        isSubscribedToPresence = false
                    )
                )
            } catch (exception: ConnectionException) {
                logHandler?.w(
                    "Failed to subscribe to presence for trackable ${trackable.id}",
                    exception
                )
                postWork(
                    createFinishAddingTrackableToPublisherWorkerSpecification(
                        isSubscribedToPresence = false
                    )
                )
            }
        }

        return properties
    }

    private fun createFinishAddingTrackableToPublisherWorkerSpecification(isSubscribedToPresence: Boolean) =
        WorkerSpecification.FinishAddingTrackableToPublisher(
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

    override fun onUnexpectedAsyncError(
        exception: Exception,
        postWork: (WorkerSpecification) -> Unit
    ) {
        if (isBeingRemoved) {
            postWork(
                WorkerSpecification.TrackableRemovalRequested(
                    trackable,
                    callbackFunction,
                    Result.failure(exception)
                )
            )
        } else {
            // If the async work fails we carry on as if it failed with a regular exception
            postWork(createFinishAddingTrackableToPublisherWorkerSpecification(isSubscribedToPresence = false))
        }
    }
}
