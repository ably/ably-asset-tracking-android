package com.ably.tracking.publisher

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.location.Location
import androidx.annotation.RequiresPermission
import com.ably.tracking.ConnectionStateChange
import com.ably.tracking.ErrorInformation
import com.ably.tracking.Handler
import com.ably.tracking.LocationUpdate
import com.ably.tracking.Resolution
import com.ably.tracking.ResultHandler
import com.ably.tracking.common.ClientTypes
import com.ably.tracking.common.PresenceAction
import com.ably.tracking.common.PresenceData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@SuppressLint("LogConditional")
internal class DefaultPublisher
@RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
constructor(
    private val ablyService: AblyService,
    private val mapboxService: MapboxService,
    resolutionPolicyFactory: ResolutionPolicy.Factory,
    private var _routingProfile: RoutingProfile
) :
    Publisher {
    private val core: CorePublisher
    private val thresholdChecker = ThresholdChecker()
    private val presenceData = PresenceData(ClientTypes.PUBLISHER)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val eventsChannel: SendChannel<Event>
    private val policy: ResolutionPolicy
    private val hooks = Hooks()
    private val methods = Methods()
    private val trackables = mutableSetOf<Trackable>()
    private val requests = mutableMapOf<String, MutableMap<Subscriber, Resolution>>()
    private val subscribers = mutableMapOf<String, MutableSet<Subscriber>>()
    private val resolutions = mutableMapOf<String, Resolution>()
    private var locationEngineResolution: Resolution
    private var isTracking: Boolean = false
    private var lastPublisherLocation: Location? = null
    private var lastSentEnhancedLocations: MutableMap<String, Location> = mutableMapOf()
    private var estimatedArrivalTimeInMilliseconds: Long? = null

    /**
     * This field will be set only when trying to set a tracking destination before receiving any [lastPublisherLocation].
     * After successfully setting the tracking destination this field will be set to NULL.
     **/
    private var destinationToSet: Destination? = null
    private var currentDestination: Destination? = null

    override val locations: SharedFlow<LocationUpdate>
        get() = core.locations
    override val connectionStates: SharedFlow<ConnectionStateChange>
        get() = core.connectionStates

    init {
        eventsChannel = createEventsChannel(scope)
        policy = resolutionPolicyFactory.createResolutionPolicy(
            hooks,
            methods
        )
        locationEngineResolution = policy.resolve(emptySet())

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

    private fun performPresenceMessage(event: PresenceMessageEvent) {
        when (event.presenceMessage.action) {
            PresenceAction.PRESENT_OR_ENTER -> {
                if (event.presenceMessage.data.type == ClientTypes.SUBSCRIBER) {
                    addSubscriber(event.presenceMessage.clientId, event.trackable, event.presenceMessage.data)
                }
            }
            PresenceAction.LEAVE_OR_ABSENT -> {
                if (event.presenceMessage.data.type == ClientTypes.SUBSCRIBER) {
                    removeSubscriber(event.presenceMessage.clientId, event.trackable)
                }
            }
            PresenceAction.UPDATE -> {
                if (event.presenceMessage.data.type == ClientTypes.SUBSCRIBER) {
                    updateSubscriber(event.presenceMessage.clientId, event.trackable, event.presenceMessage.data)
                }
            }
        }
    }

    private fun addSubscriber(id: String, trackable: Trackable, data: PresenceData) {
        val subscriber = Subscriber(id, trackable)
        if (subscribers[trackable.id] == null) {
            subscribers[trackable.id] = mutableSetOf()
        }
        subscribers[trackable.id]?.add(subscriber)
        saveOrRemoveResolutionRequest(data.resolution, trackable, subscriber)
        hooks.subscribers?.onSubscriberAdded(subscriber)
        resolveResolution(trackable)
    }

    private fun updateSubscriber(id: String, trackable: Trackable, data: PresenceData) {
        subscribers[trackable.id]?.let { subscribers ->
            subscribers.find { it.id == id }?.let { subscriber ->
                data.resolution.let { resolution ->
                    saveOrRemoveResolutionRequest(resolution, trackable, subscriber)
                    resolveResolution(trackable)
                }
            }
        }
    }

    private fun removeSubscriber(id: String, trackable: Trackable) {
        subscribers[trackable.id]?.let { subscribers ->
            subscribers.find { it.id == id }?.let { subscriber ->
                subscribers.remove(subscriber)
                requests[trackable.id]?.remove(subscriber)
                hooks.subscribers?.onSubscriberRemoved(subscriber)
                resolveResolution(trackable)
            }
        }
    }

    private fun saveOrRemoveResolutionRequest(resolution: Resolution?, trackable: Trackable, subscriber: Subscriber) {
        if (resolution != null) {
            if (requests[trackable.id] == null) {
                requests[trackable.id] = mutableMapOf()
            }
            requests[trackable.id]?.put(subscriber, resolution)
        } else {
            requests[trackable.id]?.remove(subscriber)
        }
    }

    /**
     * This method must be called from the publishers event queue.
     */
    private fun setDestination(destination: Destination) {
        // TODO is there a way to ensure we're executing in the right thread?
        lastPublisherLocation.let { currentLocation ->
            if (currentLocation != null) {
                destinationToSet = null
                removeCurrentDestination()
                currentDestination = destination
                mapboxService.setRoute(currentLocation, destination, routingProfile) {
                    enqueue(SetDestinationSuccessEvent(it))
                }
            } else {
                destinationToSet = destination
            }
        }
    }

    private fun removeCurrentDestination() {
        mapboxService.clearRoute()
        currentDestination = null
        estimatedArrivalTimeInMilliseconds = null
    }

    private fun resolveResolution(trackable: Trackable) {
        val resolutionRequests: Set<Resolution> = requests[trackable.id]?.values?.toSet() ?: emptySet()
        policy.resolve(TrackableResolutionRequest(trackable, resolutionRequests)).let { resolution ->
            resolutions[trackable.id] = resolution
            enqueue(ChangeLocationEngineResolutionEvent())
        }
    }

    @OptIn(ObsoleteCoroutinesApi::class)
    @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
    private fun createEventsChannel(scope: CoroutineScope) =
        scope.actor<Event> {
            for (event in channel) {
                when (event) {
                    is AddTrackableEvent -> {}
                    is TrackTrackableEvent -> {}
                    is RemoveTrackableEvent -> {}
                    is StopEvent -> {}
                    is StartEvent -> {}
                    is JoinPresenceSuccessEvent -> {}
                    is RawLocationChangedEvent -> {}
                    is EnhancedLocationChangedEvent -> {}
                    is RefreshResolutionPolicyEvent -> {}
                    is SetDestinationSuccessEvent -> {}
                    is PresenceMessageEvent -> {}
                    is ChangeLocationEngineResolutionEvent -> {}
                    is SetActiveTrackableEvent -> {}
                    is ChangeRoutingProfileEvent -> {}
                }
            }
        }

    /**
     * Send a failure event to the main thread, but only if the scope hasn't been cancelled.
     */
    private fun <T> callback(handler: ResultHandler<T>, errorInformation: ErrorInformation) {
        // TODO - shouldn't we somehow keep all ErrorInformation data in the exception?
        callback(handler, Result.failure(Exception(errorInformation.message)))
    }

    /**
     * Send an event to the main thread, but only if the scope hasn't been cancelled.
     */
    private fun <T> callback(handler: Handler<T>, result: T) {
        scope.launch(Dispatchers.Main) { handler(result) }
    }

    private fun enqueue(event: Event) {
        scope.launch { eventsChannel.send(event) }
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
            core.enqueue(RefreshResolutionPolicyEvent())
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
