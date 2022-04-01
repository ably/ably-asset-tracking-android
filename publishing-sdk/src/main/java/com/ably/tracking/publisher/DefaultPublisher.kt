package com.ably.tracking.publisher

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import androidx.annotation.RequiresPermission
import com.ably.tracking.LocationUpdate
import com.ably.tracking.Resolution
import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.logging.LogHandler
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

@SuppressLint("LogConditional")
internal class DefaultPublisher
@RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
constructor(
    ably: Ably,
    mapbox: Mapbox,
    resolutionPolicyFactory: ResolutionPolicy.Factory,
    routingProfile: RoutingProfile,
    logHandler: LogHandler?,
    areRawLocationsEnabled: Boolean?,
    sendResolutionEnabled: Boolean,
    constantLocationEngineResolution: Resolution?,
) :
    Publisher {
    private val core: CorePublisher

    override val active: Trackable?
        get() = core.active
    override var routingProfile: RoutingProfile
        get() = core.routingProfile
        set(value) = core.changeRoutingProfile(value)
    override val locations: SharedFlow<LocationUpdate>
        get() = core.locations
    override val trackables: SharedFlow<Set<Trackable>>
        get() = core.trackables
    override val locationHistory: SharedFlow<LocationHistoryData>
        get() = core.locationHistory

    init {
        core = createCorePublisher(
            ably,
            mapbox,
            resolutionPolicyFactory,
            routingProfile,
            logHandler,
            areRawLocationsEnabled,
            sendResolutionEnabled,
            constantLocationEngineResolution,
        )
    }

    override suspend fun track(trackable: Trackable): StateFlow<TrackableState> {
        return suspendCoroutine { continuation ->
            core.trackTrackable(trackable) {
                try {
                    continuation.resume(it.getOrThrow())
                } catch (exception: Exception) {
                    continuation.resumeWithException(exception)
                }
            }
        }
    }

    override suspend fun add(trackable: Trackable): StateFlow<TrackableState> {
        return suspendCoroutine { continuation ->
            core.addTrackable(trackable) {
                try {
                    continuation.resume(it.getOrThrow())
                } catch (exception: Exception) {
                    continuation.resumeWithException(exception)
                }
            }
        }
    }

    override suspend fun remove(trackable: Trackable): Boolean {
        return suspendCoroutine { continuation ->
            core.removeTrackable(trackable) {
                try {
                    continuation.resume(it.getOrThrow())
                } catch (exception: Exception) {
                    continuation.resumeWithException(exception)
                }
            }
        }
    }

    override suspend fun stop(timeoutInMilliseconds: Long) {
        suspendCoroutine<Unit> { continuation ->
            core.stop(timeoutInMilliseconds) {
                try {
                    continuation.resume(it.getOrThrow())
                } catch (exception: Exception) {
                    continuation.resumeWithException(exception)
                }
            }
        }
    }

    override fun getTrackableState(trackableId: String): StateFlow<TrackableState>? =
        core.trackableStateFlows[trackableId]
}
