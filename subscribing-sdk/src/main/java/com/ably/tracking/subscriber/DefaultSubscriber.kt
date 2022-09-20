package com.ably.tracking.subscriber

import com.ably.tracking.LocationUpdate
import com.ably.tracking.Resolution
import com.ably.tracking.TrackableState
import com.ably.tracking.annotations.Experimental
import com.ably.tracking.common.Ably
import com.ably.tracking.subscriber.workerqueue.WorkerParams
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

internal class DefaultSubscriber(
    ably: Ably,
    resolution: Resolution?,
    trackableId: String,
) : Subscriber {
    private val core: CoreSubscriber

    override val locations: SharedFlow<LocationUpdate>
        get() = core.enhancedLocations

    override val rawLocations: SharedFlow<LocationUpdate>
        get() = core.rawLocations

    override val trackableStates: StateFlow<TrackableState>
        get() = core.trackableStates

    @Experimental
    override val publisherPresence: StateFlow<Boolean>
        get() = core.publisherPresence

    override val resolutions: SharedFlow<Resolution>
        get() = core.resolutions

    override val nextLocationUpdateIntervals: SharedFlow<Long>
        get() = core.nextLocationUpdateIntervals

    init {
        core = createCoreSubscriber(ably, resolution, trackableId)
    }

    /**
     * This method must be run before running any other method from [DefaultSubscriber].
     */
    suspend fun start() {
        suspendCoroutine<Unit> { continuation ->
            core.enqueue(
                WorkerParams.Start {
                    try {
                        continuation.resume(it.getOrThrow())
                    } catch (exception: Exception) {
                        continuation.resumeWithException(exception)
                    }
                }
            )
        }
    }

    override suspend fun resolutionPreference(resolution: Resolution?) {
        // send change request over channel and wait for the result
        suspendCoroutine<Unit> { continuation ->
            core.enqueue(WorkerParams.ChangeResolution(resolution) {
                try {
                    continuation.resume(it.getOrThrow())
                } catch (exception: Exception) {
                    continuation.resumeWithException(exception)
                }
            })
        }
    }

    override suspend fun stop() {
        // send stop request over channel and wait for the result
        suspendCoroutine<Unit> { continuation ->
            core.enqueue(
                WorkerParams.Stop {
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
