package com.ably.tracking.subscriber

import com.ably.tracking.AssetStatusListener
import com.ably.tracking.LocationUpdate
import com.ably.tracking.LocationUpdateListener
import com.ably.tracking.Resolution
import kotlinx.coroutines.flow.SharedFlow
import timber.log.Timber
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal class DefaultSubscriber(
    ablyService: AblyService,
    resolution: Resolution?
) : Subscriber {
    private val core: CoreSubscriberContract

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
                if (it.isSuccess) {
                    continuation.resume(Unit)
                } else {
                    continuation.resumeWithException(Exception("Changing resolution event failed"))
                }
            })
        }
    }

    override suspend fun stop() {
        // send stop request over channel and wait for the result
        suspendCoroutine<Unit> { continuation ->
            core.request(StopEvent {
                if (it.isSuccess) {
                    continuation.resume(Unit)
                } else {
                    continuation.resumeWithException(Exception("Stopping failed"))
                }
            })
        }
    }

    override fun sendChangeRequestAsync(resolution: Resolution): CompletableFuture<Void> {
        TODO()
//        return scope.async { performChangeResolution(resolution) }.asCompletableFuture()
    }

    override fun addEnhancedLocationListener(listener: LocationUpdateListener) {
        TODO("Not yet implemented")
    }

    override fun addListener(listener: AssetStatusListener) {
        TODO("Not yet implemented")
    }

    override fun stopAsync(): CompletableFuture<Void> {
        TODO("Not yet implemented")
//        stop() { listener.onResult(it.toJava()) }
    }
}
