package com.ably.tracking.publisher

import android.Manifest
import android.location.Location
import androidx.annotation.RequiresPermission
import com.ably.tracking.ConnectionStateChange
import com.ably.tracking.LocationUpdate
import com.ably.tracking.Resolution
import com.ably.tracking.common.ClientTypes
import com.ably.tracking.common.PresenceData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

internal interface CorePublisher {
    fun enqueue(event: AdhocEvent)
    fun request(request: Request)
    val locations: SharedFlow<LocationUpdate>
    val connectionStates: SharedFlow<ConnectionStateChange>
}

@RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
internal fun createCorePublisher(
    ablyService: AblyService,
    mapboxService: MapboxService,
    resolutionPolicyFactory: ResolutionPolicy.Factory
): CorePublisher {
    return DefaultCorePublisher(ablyService, mapboxService, resolutionPolicyFactory)
}

private class DefaultCorePublisher
@RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
constructor(
    private val ablyService: AblyService,
    private val mapboxService: MapboxService,
    resolutionPolicyFactory: ResolutionPolicy.Factory
) : CorePublisher {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val sendEventChannel: SendChannel<Event>
    private val _locations = MutableSharedFlow<LocationUpdate>()
    private val _connectionStates = MutableSharedFlow<ConnectionStateChange>()
    private val thresholdChecker = ThresholdChecker()
    private val policy: ResolutionPolicy
    private val hooks = Hooks()
    private val methods = Methods()
    override val locations: SharedFlow<LocationUpdate>
        get() = _locations.asSharedFlow()
    override val connectionStates: SharedFlow<ConnectionStateChange>
        get() = _connectionStates.asSharedFlow()

    init {
        policy = resolutionPolicyFactory.createResolutionPolicy(
            hooks,
            methods
        )
        val channel = Channel<Event>()
        sendEventChannel = channel
        scope.launch {
            coroutineScope {
                sequenceEventsQueue(channel)
            }
        }
        ablyService.subscribeForAblyStateChange { state -> scope.launch { _connectionStates.emit(state) } }
    }

    override fun enqueue(event: AdhocEvent) {
        scope.launch { sendEventChannel.send(event) }
    }

    override fun request(request: Request) {
        scope.launch { sendEventChannel.send(request) }
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    private fun CoroutineScope.sequenceEventsQueue(receiveEventChannel: ReceiveChannel<Event>) {
        launch {
            // state
            var isTracking = false
            val trackables = mutableSetOf<Trackable>()
            val resolutions = mutableMapOf<String, Resolution>()
            val lastSentEnhancedLocations: MutableMap<String, Location> = mutableMapOf()
            var estimatedArrivalTimeInMilliseconds: Long? = null
            val active: Trackable? = null
            val presenceData = PresenceData(ClientTypes.PUBLISHER)

            // processing
            for (event in receiveEventChannel) {
                when (event) {
                    is StartEvent -> {
                        if (!isTracking) {
                            isTracking = true

                            Timber.e("startLocationUpdates")

                            mapboxService.startTrip()
                        }
                    }
                    is EnhancedLocationChangedEvent -> {
                        for (trackable in trackables) {
                            if (shouldSendLocation(
                                    event.locationUpdate.location,
                                    lastSentEnhancedLocations[trackable.id],
                                    resolutions[trackable.id]
                                )
                            ) {
                                lastSentEnhancedLocations[trackable.id] = event.locationUpdate.location
                                ablyService.sendEnhancedLocation(trackable.id, event.locationUpdate)
                            }
                        }
                        scope.launch { _locations.emit(event.locationUpdate) }
                        checkThreshold(event.locationUpdate.location, active, estimatedArrivalTimeInMilliseconds)
                    }
                }
            }
        }
    }

    private fun checkThreshold(
        currentLocation: Location,
        activeTrackable: Trackable?,
        estimatedArrivalTimeInMilliseconds: Long?
    ) {
        methods.threshold?.let { threshold ->
            if (thresholdChecker.isThresholdReached(
                    threshold,
                    currentLocation,
                    System.currentTimeMillis(),
                    activeTrackable?.destination,
                    estimatedArrivalTimeInMilliseconds
                )
            ) {
                methods.onProximityReached()
            }
        }
    }

    private fun shouldSendLocation(
        currentLocation: Location,
        lastSentLocation: Location?,
        resolution: Resolution?
    ): Boolean {
        return if (resolution != null && lastSentLocation != null) {
            val timeSinceLastSentLocation = currentLocation.timeFrom(lastSentLocation)
            val distanceFromLastSentLocation = currentLocation.distanceInMetersFrom(lastSentLocation)
            return distanceFromLastSentLocation >= resolution.minimumDisplacement &&
                timeSinceLastSentLocation >= resolution.desiredInterval
        } else {
            true
        }
    }

    private inner class Hooks : ResolutionPolicy.Hooks {
        var trackables: ResolutionPolicy.Hooks.TrackableSetListener? = null
        var subscribers: ResolutionPolicy.Hooks.SubscriberSetListener? = null

        override fun trackables(listener: ResolutionPolicy.Hooks.TrackableSetListener) {
            trackables = listener
        }

        override fun subscribers(listener: ResolutionPolicy.Hooks.SubscriberSetListener) {
            subscribers = listener
        }
    }

    private inner class Methods : ResolutionPolicy.Methods {
        var proximityHandler: ResolutionPolicy.Methods.ProximityHandler? = null
        var threshold: Proximity? = null
        override fun refresh() {
            enqueue(RefreshResolutionPolicyEvent())
        }

        override fun setProximityThreshold(
            threshold: Proximity,
            handler: ResolutionPolicy.Methods.ProximityHandler
        ) {
            this.proximityHandler = handler
            this.threshold = threshold
        }

        override fun cancelProximityThreshold() {
            proximityHandler?.onProximityCancelled()
        }

        fun onProximityReached() {
            threshold?.let { proximityHandler?.onProximityReached(it) }
        }
    }
}
