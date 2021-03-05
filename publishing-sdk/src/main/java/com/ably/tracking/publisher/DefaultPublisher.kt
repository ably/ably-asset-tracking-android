package com.ably.tracking.publisher

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import androidx.annotation.RequiresPermission
import com.ably.tracking.AssetState
import com.ably.tracking.LocationUpdate
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
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
    routingProfile: RoutingProfile,
    batteryDataProvider: BatteryDataProvider
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
    override val trackables: SharedFlow<Set<Trackable>>
        get() = core.trackables
    override val locationHistory: SharedFlow<LocationHistoryData>
        get() = core.locationHistory

    init {
        core = createCorePublisher(ably, mapbox, resolutionPolicyFactory, routingProfile, batteryDataProvider)

        core.enqueue(StartEvent())
    }

    override suspend fun track(trackable: Trackable): StateFlow<AssetState> {
        return suspendCoroutine { continuation ->
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

    override suspend fun add(trackable: Trackable): StateFlow<AssetState> {
        return suspendCoroutine { continuation ->
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

    override fun getAssetState(trackableId: String): StateFlow<AssetState>? = core.assetStateFlows[trackableId]
}
