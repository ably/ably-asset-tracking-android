package com.ably.tracking.subscriber

import com.ably.tracking.LocationUpdate
import com.ably.tracking.Resolution
import kotlinx.coroutines.flow.SharedFlow
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal class DefaultSubscriber(
    ablyService: AblyService,
    resolution: Resolution?
) : Subscriber {
    private val core: CoreSubscriber

    override val enhancedLocations: SharedFlow<LocationUpdate>
        get() = core.enhancedLocations

    override val assetStatuses: SharedFlow<Boolean>
        get() = core.assetStatuses

    init {
        Timber.w("Started.")

        core = createCoreSubscriber(ablyService, resolution)
        core.enqueue(StartEvent())
    }

    override suspend fun sendChangeRequest(resolution: Resolution) {
        // send change request over channel and wait for the result
        suspendCoroutine<Unit> { continuation ->
            core.request(ChangeResolutionEvent(resolution) {
                try {
                    continuation.resume(it.getOrThrow())
                } catch (exception: Exception){
                    continuation.resumeWithException(exception)
                }
            })
        }
    }

    override suspend fun stop() {
        // send stop request over channel and wait for the result
        suspendCoroutine<Unit> { continuation ->
            core.request(StopEvent {
                try {
                    continuation.resume(it.getOrThrow())
                } catch (exception: Exception){
                    continuation.resumeWithException(exception)
                }
            })
        }
    }
}
