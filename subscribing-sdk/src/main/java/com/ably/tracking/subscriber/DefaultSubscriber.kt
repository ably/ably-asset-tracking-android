package com.ably.tracking.subscriber

import com.ably.tracking.LocationUpdate
import com.ably.tracking.Resolution
import com.ably.tracking.TrackableState
import com.ably.tracking.annotations.Experimental
import com.ably.tracking.common.Ably
import com.ably.tracking.common.logging.v
import com.ably.tracking.common.logging.w
import com.ably.tracking.common.wrapInResultCallback
import com.ably.tracking.logging.LogHandler
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

internal class DefaultSubscriber(
    ably: Ably,
    resolution: Resolution?,
    trackableId: String,
    private val logHandler: LogHandler?,
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
        logHandler?.v("Created a subscriber instance")
    }

    /**
     * This method must be run before running any other method from [DefaultSubscriber].
     */
    suspend fun start() {
        logHandler?.v("Subscriber start operation started")
        suspendCoroutine<Unit> { continuation ->
            core.enqueue(
                WorkerSpecification.StartConnection(
                    continuation.wrapInResultCallback(
                        onSuccess = { logHandler?.v("Subscriber start operation succeeded") },
                        onError = { logHandler?.w("Subscriber start operation failed", it) },
                    )
                )
            )
        }
    }

    override suspend fun resolutionPreference(resolution: Resolution?) {
        logHandler?.v("Subscriber resolutionPreference operation started")
        // send change request over channel and wait for the result
        suspendCoroutine<Unit> { continuation ->
            core.enqueue(
                WorkerSpecification.ChangeResolution(
                    resolution,
                    continuation.wrapInResultCallback(
                        onSuccess = { logHandler?.v("Subscriber resolutionPreference operation succeeded") },
                        onError = { logHandler?.w("Subscriber resolutionPreference operation failed", it) },
                    ),
                )
            )
        }
    }

    override suspend fun stop() {
        logHandler?.v("Subscriber stop operation started")
        // send stop request over channel and wait for the result
        suspendCoroutine<Unit> { continuation ->
            core.enqueue(
                WorkerSpecification.StopConnection(
                    continuation.wrapInResultCallback(
                        onSuccess = { logHandler?.v("Subscriber stop operation succeeded") },
                        onError = { logHandler?.w("Subscriber stop operation failed", it) },
                    )
                )
            )
        }
    }
}
