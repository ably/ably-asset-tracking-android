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
    ably: Ably,
    mapbox: Mapbox,
    resolutionPolicyFactory: ResolutionPolicy.Factory,
    routingProfile: RoutingProfile
) :
    Publisher {
    private val core: CorePublisher

    override val active: Trackable?
        get() = core.active
    override var routingProfile: RoutingProfile
        get() = core.routingProfile
        set(value) = core.enqueue(ChangeRoutingProfileEvent(value))
    override val locations: SharedFlow<LocationUpdate>
        get() = core.locations
    override val connectionStates: SharedFlow<ConnectionStateChange>
        get() = core.connectionStates
    override val trackables: SharedFlow<Set<Trackable>>
        get() = core.trackables

    init {
        Timber.w("Started.")

        core = createCorePublisher(ably, mapbox, resolutionPolicyFactory, routingProfile)

        core.enqueue(StartEvent())
    }

    override suspend fun track(trackable: Trackable) {
        suspendCoroutine<Unit> { continuation ->
            core.request(
                TrackTrackableEvent(trackable) {
                    try {
                        continuation.resume(it.getOrThrow())
                    } catch (exception: Exception) {
                        continuation.resumeWithException(exception)
                    }
                }
            )
        }
    }

    override suspend fun add(trackable: Trackable) {
        suspendCoroutine<Unit> { continuation ->
            core.request(
                AddTrackableEvent(trackable) {
                    try {
                        continuation.resume(it.getOrThrow())
                    } catch (exception: Exception) {
                        continuation.resumeWithException(exception)
                    }
                }
            )
        }
    }

    override suspend fun remove(trackable: Trackable): Boolean {
        return suspendCoroutine { continuation ->
            core.request(
                RemoveTrackableEvent(trackable) {
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
