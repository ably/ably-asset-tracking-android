package com.ably.tracking.publisher

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import androidx.annotation.RequiresPermission
import com.ably.tracking.LocationUpdate
import com.ably.tracking.Resolution
import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.common.logging.createLoggingTag
import com.ably.tracking.common.logging.v
import com.ably.tracking.common.logging.w
import com.ably.tracking.common.wrapInResultCallback
import com.ably.tracking.logging.LogHandler
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
    private val logHandler: LogHandler?,
    areRawLocationsEnabled: Boolean?,
    sendResolutionEnabled: Boolean,
    constantLocationEngineResolution: Resolution?,
) :
    Publisher {
    private val core: CorePublisher
    private val TAG = createLoggingTag(this)

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
        logHandler?.v("$TAG Created a publisher instance")
    }

    override suspend fun track(trackable: Trackable): StateFlow<TrackableState> {
        logHandler?.v("$TAG Publisher track operation started")
        return suspendCoroutine { continuation ->
            core.trackTrackable(
                trackable,
                continuation.wrapInResultCallback(
                    onSuccess = { logHandler?.v("$TAG Publisher track operation succeeded") },
                    onError = { logHandler?.w("$TAG Publisher track operation failed", it) },
                ),
            )
        }
    }

    override suspend fun add(trackable: Trackable): StateFlow<TrackableState> {
        logHandler?.v("$TAG Publisher add operation started")
        return suspendCoroutine { continuation ->
            core.addTrackable(
                trackable,
                continuation.wrapInResultCallback(
                    onSuccess = { logHandler?.v("$TAG Publisher add operation succeeded") },
                    onError = { logHandler?.w("$TAG Publisher add operation failed", it) },
                ),
            )
        }
    }

    override suspend fun remove(trackable: Trackable): Boolean {
        logHandler?.v("$TAG Publisher remove operation started")
        return suspendCoroutine { continuation ->
            core.removeTrackable(
                trackable,
                continuation.wrapInResultCallback(
                    onSuccess = { logHandler?.v("$TAG Publisher remove operation succeeded") },
                    onError = { logHandler?.w("$TAG Publisher remove operation failed", it) },
                ),
            )
        }
    }

    override suspend fun stop(timeoutInMilliseconds: Long) {
        logHandler?.v("$TAG Publisher stop operation started")
        suspendCoroutine<Unit> { continuation ->
            core.stop(
                timeoutInMilliseconds,
                continuation.wrapInResultCallback(
                    onSuccess = { logHandler?.v("$TAG Publisher stop operation succeeded") },
                    onError = { logHandler?.w("$TAG Publisher stop operation failed", it) },
                ),
            )
        }
    }

    override fun getTrackableState(trackableId: String): StateFlow<TrackableState>? =
        core.trackableStateFlows[trackableId]
}
