package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.ConnectionException
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.common.logging.w
import com.ably.tracking.logging.LogHandler
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.PublisherState
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.results.ConnectionCreatedWorkResult
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class ConnectionCreatedWorker(
    private val trackable: Trackable,
    private val callbackFunction: AddTrackableCallbackFunction,
    private val ably: Ably,
    private val logHandler: LogHandler?,
    private val presenceUpdateListener: ((presenceMessage: PresenceMessage) -> Unit),
    private val channelStateChangeListener: ((connectionStateChange: ConnectionStateChange) -> Unit),
) : Worker {
    override fun doWork(properties: PublisherProperties): SyncAsyncResult {
        if (properties.state == PublisherState.CONNECTING) {
            // If we've made up this far it means the [AddTrackableWorker] succeeded and there's a working Ably connection
            properties.state = PublisherState.CONNECTED
        }
        if (properties.trackableRemovalGuard.isMarkedForRemoval(trackable)) {
            // Leave Ably channel.
            val presenceData = properties.presenceData.copy()
            return SyncAsyncResult(
                asyncWork = {
                    val result = ably.disconnect(trackable.id, presenceData)
                    ConnectionCreatedWorkResult.RemovalRequested(trackable, callbackFunction, result)
                }
            )
        }
        return SyncAsyncResult(
            asyncWork = {
                val subscribeToPresenceResult = subscribeToPresenceMessages()
                try {
                    subscribeToPresenceResult.getOrThrow()
                    ConnectionCreatedWorkResult.PresenceSuccess(
                        trackable,
                        callbackFunction,
                        presenceUpdateListener,
                        channelStateChangeListener
                    )
                } catch (exception: ConnectionException) {
                    logHandler?.w("Failed to subscribe to presence for trackable ${trackable.id}", exception)
                    ConnectionCreatedWorkResult.PresenceFail(
                        trackable,
                        callbackFunction,
                        presenceUpdateListener,
                        channelStateChangeListener,
                    )
                }
            }
        )
    }

    private suspend fun subscribeToPresenceMessages(): Result<Unit> {
        return suspendCoroutine { continuation ->
            ably.subscribeForPresenceMessages(trackable.id, presenceUpdateListener) { result ->
                continuation.resume(result)
            }
        }
    }

    override fun doWhenStopped(exception: Exception) {
        callbackFunction(Result.failure(exception))
    }
}
