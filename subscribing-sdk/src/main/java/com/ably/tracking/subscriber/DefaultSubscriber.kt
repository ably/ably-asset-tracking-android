package com.ably.tracking.subscriber

import com.ably.tracking.LocationUpdate
import com.ably.tracking.Resolution
import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal class DefaultSubscriber(
    ably: Ably,
    resolution: Resolution?,
    trackableId: String
) : Subscriber {
    private val core: CoreSubscriber

    override val locations: SharedFlow<LocationUpdate>
        get() = core.enhancedLocations

    override val trackableStates: StateFlow<TrackableState>
        get() = core.trackableStates

    init {
        Timber.w("Started.")

        core = createCoreSubscriber(ably, resolution, trackableId)
    }

    /**
     * This method must be run before running any other method from [DefaultSubscriber].
     */
    suspend fun start() {
        suspendCoroutine<Unit> { continuation ->
            core.request(
                StartEvent {
                    try {
                        continuation.resume(it.getOrThrow())
                    } catch (exception: Exception) {
                        continuation.resumeWithException(exception)
                    }
                }
            )
        }
    }

    override suspend fun sendChangeRequest(resolution: Resolution) {
        // send change request over channel and wait for the result
        suspendCoroutine<Unit> { continuation ->
            core.request(
                ChangeResolutionEvent(resolution) {
                    try {
                        continuation.resume(it.getOrThrow())
                    } catch (exception: Exception) {
                        continuation.resumeWithException(exception)
                    }
                }
            )
        }
    }

    override suspend fun stop() {
        // send stop request over channel and wait for the result
        suspendCoroutine<Unit> { continuation ->
            core.request(
                StopEvent {
                    try {
                        continuation.resume(it.getOrThrow())
                    } catch (exception: Exception) {
                        continuation.resumeWithException(exception)
                    }
                }
            )
        }
    }
}
