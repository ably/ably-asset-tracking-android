package com.ably.tracking.publisher

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import androidx.annotation.RequiresPermission
import com.ably.tracking.ConnectionStateChange
import com.ably.tracking.LocationUpdate
import kotlinx.coroutines.flow.SharedFlow
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@SuppressLint("LogConditional")
internal class DefaultPublisher
@RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
constructor(
    ablyService: AblyService,
    mapboxService: MapboxService,
    resolutionPolicyFactory: ResolutionPolicy.Factory,
    private var _routingProfile: RoutingProfile
) :
    Publisher {
    private val core: CorePublisher

    override val locations: SharedFlow<LocationUpdate>
        get() = core.locations
    override val connectionStates: SharedFlow<ConnectionStateChange>
        get() = core.connectionStates

    init {
        Timber.w("Started.")

        core = createCorePublisher(ablyService, mapboxService, resolutionPolicyFactory, _routingProfile)

        core.enqueue(StartEvent())
    }

    override suspend fun track(trackable: Trackable) {
        suspendCoroutine<Unit> { continuation ->
            core.request(TrackTrackableEvent(trackable) {
                try {
                    continuation.resume(it.getOrThrow())
                } catch (exception: Exception) {
                    continuation.resumeWithException(exception)
                }
            })
        }
    }

    override suspend fun add(trackable: Trackable) {
        suspendCoroutine<Unit> { continuation ->
            core.request(AddTrackableEvent(trackable) {
                try {
                    continuation.resume(it.getOrThrow())
                } catch (exception: Exception) {
                    continuation.resumeWithException(exception)
                }
            })
        }
    }

    override suspend fun remove(trackable: Trackable): Boolean {
        return suspendCoroutine { continuation ->
            core.request(RemoveTrackableEvent(trackable) {
                try {
                    continuation.resume(it.getOrThrow())
                } catch (exception: Exception) {
                    continuation.resumeWithException(exception)
                }
            })
        }
    }

    // TODO - get active from the CorePublisher
    override var active: Trackable? = null

    override var routingProfile: RoutingProfile
        // TODO - get routing profile value from the CorePublisher
        get() = _routingProfile
        set(value) {
            core.enqueue(ChangeRoutingProfileEvent(value))
        }

    override suspend fun stop() {
        suspendCoroutine<Unit> { continuation ->
            core.request(StopEvent {
                try {
                    continuation.resume(it.getOrThrow())
                } catch (exception: Exception) {
                    continuation.resumeWithException(exception)
                }
            })
        }
    }
}
