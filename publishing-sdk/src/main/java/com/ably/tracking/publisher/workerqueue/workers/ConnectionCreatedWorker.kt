package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.ConnectionException
import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.common.ResultCallbackFunction
import com.ably.tracking.publisher.ConnectionForTrackableCreatedEvent
import com.ably.tracking.publisher.PublisherProperties
import com.ably.tracking.publisher.Request
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.publisher.workerqueue.results.ConnectionCreatedWorkResult
import com.ably.tracking.publisher.workerqueue.results.SyncAsyncResult
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.flow.StateFlow

internal class ConnectionCreatedWorker(
    private val trackable: Trackable,
    private val callbackFunction: ResultCallbackFunction<StateFlow<TrackableState>>,
    private val ably: Ably,
    private val presenceUpdateListener: ((presenceMessage: PresenceMessage) -> Unit),
    private val channelStateChangeListener: ((connectionStateChange: ConnectionStateChange) -> Unit),
) : Worker {
    override val event: Request<*>
        get() = ConnectionForTrackableCreatedEvent(trackable, callbackFunction)

    override fun doWork(properties: PublisherProperties): SyncAsyncResult {
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
        val presenceData = properties.presenceData.copy()
        return SyncAsyncResult(
            asyncWork = {
                subscribeToPresenceMessages(presenceData)
            }
        )
    }

    private suspend fun subscribeToPresenceMessages(presenceData: PresenceData): ConnectionCreatedWorkResult {
        return suspendCoroutine { continuation ->
            ably.subscribeForPresenceMessages(
                trackableId = trackable.id,
                listener = presenceUpdateListener,
                callback = { result ->
                    try {
                        result.getOrThrow()
                        continuation.resume(
                            ConnectionCreatedWorkResult.PresenceSuccess(
                                trackable,
                                callbackFunction,
                                presenceUpdateListener,
                                channelStateChangeListener,
                            )
                        )
                    } catch (exception: ConnectionException) {
                        ably.disconnect(trackable.id, presenceData) {
                            continuation.resume(
                                ConnectionCreatedWorkResult.PresenceFail(
                                    trackable,
                                    callbackFunction,
                                    exception
                                )
                            )
                        }
                    }
                }
            )
        }
    }
}
