package com.ably.tracking.subscriber

import com.ably.tracking.LocationUpdate
import com.ably.tracking.Resolution
import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ClientTypes
import com.ably.tracking.common.ConnectionState
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.PresenceAction
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.common.createSingleThreadDispatcher
import com.ably.tracking.common.workerqueue.Properties
import com.ably.tracking.common.workerqueue.WorkerQueue
import com.ably.tracking.subscriber.workerqueue.WorkerFactory
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal interface CoreSubscriber {
    fun enqueue(workerSpecification: WorkerSpecification)
    val enhancedLocations: SharedFlow<LocationUpdate>
    val rawLocations: SharedFlow<LocationUpdate>
    val trackableStates: StateFlow<TrackableState>
    val publisherPresence: StateFlow<Boolean>
    val resolutions: SharedFlow<Resolution>
    val nextLocationUpdateIntervals: SharedFlow<Long>
}

internal interface SubscriberInteractor {
    fun subscribeForRawEvents(presenceData: PresenceData)
    fun subscribeForEnhancedEvents(presenceData: PresenceData)
    fun subscribeForChannelState()
    fun notifyAssetIsOffline()
}

internal fun createCoreSubscriber(
    ably: Ably,
    initialResolution: Resolution? = null,
    trackableId: String,
): CoreSubscriber {
    return DefaultCoreSubscriber(ably, initialResolution, trackableId)
}

/**
 * This is a private static single thread dispatcher that will be used for all the [Subscriber] instances.
 */
private val singleThreadDispatcher = createSingleThreadDispatcher()

private class DefaultCoreSubscriber(
    private val ably: Ably,
    initialResolution: Resolution?,
    private val trackableId: String,
) :
    CoreSubscriber, SubscriberInteractor {
    private val workerQueue: WorkerQueue<SubscriberProperties, WorkerSpecification>

    private val eventFlows: SubscriberProperties.EventFlows
    private val properties: SubscriberProperties

    override val enhancedLocations: SharedFlow<LocationUpdate>
        get() = eventFlows.enhancedLocations.asSharedFlow()

    override val rawLocations: SharedFlow<LocationUpdate>
        get() = eventFlows.rawLocations.asSharedFlow()

    override val trackableStates: StateFlow<TrackableState>
        get() = eventFlows.trackableStateFlow.asStateFlow()

    override val publisherPresence: StateFlow<Boolean>
        get() = eventFlows.publisherPresenceStateFlow

    override val resolutions: SharedFlow<Resolution>
        get() = eventFlows.resolutions.asSharedFlow()

    override val nextLocationUpdateIntervals: SharedFlow<Long>
        get() = eventFlows.nextLocationUpdateIntervals.asSharedFlow()

    init {
        val workerFactory = WorkerFactory(this, ably, trackableId)
        eventFlows = SubscriberProperties.EventFlows()
        properties = SubscriberProperties(initialResolution, eventFlows)
        workerQueue = WorkerQueue(
            properties = properties,
            scope = eventFlows.scope,
            workerFactory = workerFactory,
            copyProperties = { copy() },
            getStoppedException = { SubscriberStoppedException() }
        )

        ably.subscribeForAblyStateChange { enqueue(WorkerSpecification.UpdateConnectionState(it)) }
    }

    override fun enqueue(workerSpecification: WorkerSpecification) {
        workerQueue.enqueue(workerSpecification)
    }

    override fun subscribeForChannelState() {
        ably.subscribeForChannelStateChange(trackableId) {
            enqueue(WorkerSpecification.UpdateChannelConnectionState(it))
        }
    }

    override fun subscribeForEnhancedEvents(presenceData: PresenceData) {
        ably.subscribeForEnhancedEvents(trackableId, presenceData) {
            eventFlows.scope.launch { eventFlows.enhancedLocations.emit(it) }
        }
    }

    override fun subscribeForRawEvents(presenceData: PresenceData) {
        ably.subscribeForRawEvents(trackableId, presenceData) {
            eventFlows.scope.launch { eventFlows.rawLocations.emit(it) }
        }
    }

    override fun notifyAssetIsOffline() {
        // TODO what is this method achieving, why is it not in normal flow?
        eventFlows.scope.launch { eventFlows.trackableStateFlow.emit(TrackableState.Offline()) }
    }
}

private class PendingResolutions {
    private var resolutions: MutableList<Resolution> = ArrayList()

    fun add(resolution: Resolution) {
        resolutions.add(resolution)
    }

    fun drain(): Array<Resolution> {
        val array = resolutions.toTypedArray()
        resolutions.clear()
        return array
    }
}

internal data class SubscriberProperties private constructor(
    var presenceData: PresenceData,
    private val stateFlows: EventFlows,

    override var isStopped: Boolean = false,

    private var presentPublisherMemberKeys: MutableSet<String> = HashSet(),
    private var lastEmittedIsPublisherVisible: Boolean? = null,
    private var lastEmittedTrackableState: TrackableState = TrackableState.Offline(),
    private var lastConnectionStateChange: ConnectionStateChange =
        ConnectionStateChange(ConnectionState.OFFLINE, null),
    private var lastChannelConnectionStateChange: ConnectionStateChange =
        ConnectionStateChange(ConnectionState.OFFLINE, null),
    private var pendingPublisherResolutions: PendingResolutions = PendingResolutions(),
) : Properties {
    internal constructor(
        initialResolution: Resolution?,
        stateFlows: EventFlows,
    ) : this(PresenceData(ClientTypes.SUBSCRIBER, initialResolution), stateFlows)

    fun updateForConnectionStateChangeAndThenEmitEventsIfRequired(stateChange: ConnectionStateChange) {
        lastConnectionStateChange = stateChange
        emitEventsIfRequired()
    }

    fun updateForChannelConnectionStateChangeAndThenEmitEventsIfRequired(stateChange: ConnectionStateChange) {
        lastChannelConnectionStateChange = stateChange
        emitEventsIfRequired()
    }

    fun updateForPresenceMessage(presenceMessage: PresenceMessage) {
        if (presenceMessage.data.type != ClientTypes.PUBLISHER) {
            // We are only interested in presence updates from publishers.
            return
        }

        if (presenceMessage.action == PresenceAction.LEAVE_OR_ABSENT) {
            // LEAVE or ABSENT
            presentPublisherMemberKeys.remove(presenceMessage.memberKey)
        } else {
            // PRESENT, ENTER or UDPATE
            presentPublisherMemberKeys.add(presenceMessage.memberKey)
            presenceMessage.data.resolution?.let { publisherResolution ->
                pendingPublisherResolutions.add(publisherResolution)
            }
        }
    }

    fun emitEventsIfRequired() {
        val isAPublisherPresent = (presentPublisherMemberKeys.size > 0)

        val trackableState = when (lastConnectionStateChange.state) {
            ConnectionState.ONLINE -> {
                when (lastChannelConnectionStateChange.state) {
                    ConnectionState.ONLINE -> if (isAPublisherPresent) TrackableState.Online else TrackableState.Offline()
                    ConnectionState.OFFLINE -> TrackableState.Offline()
                    ConnectionState.FAILED -> TrackableState.Failed(lastChannelConnectionStateChange.errorInformation!!) // are we sure error information will always be present?
                }
            }
            ConnectionState.OFFLINE -> TrackableState.Offline()
            ConnectionState.FAILED -> TrackableState.Failed(lastConnectionStateChange.errorInformation!!) // are we sure error information will always be present?
        }

        if (trackableState != lastEmittedTrackableState) {
            lastEmittedTrackableState = trackableState
            stateFlows.scope.launch { stateFlows.trackableStateFlow.emit(trackableState) }
        }

        // It is possible for presentPublisherMemberKeys to not be empty, even when we have no connectivity from our side,
        // because we've had presence entry events without subsequent leave events.
        // Therefore, from the perspective of a user consuming events from publisherPresenceStateFlow, what matters
        // is what we're computing for isPublisherVisible (not the simple isAPublisherPresent).
        val isPublisherVisible = (isAPublisherPresent && lastConnectionStateChange.state == ConnectionState.ONLINE)
        if (null == lastEmittedIsPublisherVisible || lastEmittedIsPublisherVisible!! != isPublisherVisible) {
            lastEmittedIsPublisherVisible = isPublisherVisible
            stateFlows.scope.launch { stateFlows.publisherPresenceStateFlow.emit(isPublisherVisible) }
        }

        val publisherResolutions = pendingPublisherResolutions.drain()
        if (publisherResolutions.size > 0) {
            stateFlows.scope.launch {
                for (publisherResolution in publisherResolutions) {
                    stateFlows.resolutions.emit(publisherResolution)
                    stateFlows.nextLocationUpdateIntervals.emit(publisherResolution.desiredInterval)
                }
            }
        }
    }

    internal data class EventFlows constructor(
        val scope: CoroutineScope = CoroutineScope(singleThreadDispatcher + SupervisorJob()),
        val enhancedLocations: MutableSharedFlow<LocationUpdate> = MutableSharedFlow(replay = 1),
        val rawLocations: MutableSharedFlow<LocationUpdate> = MutableSharedFlow(replay = 1),
        val trackableStateFlow: MutableStateFlow<TrackableState> = MutableStateFlow(TrackableState.Offline()),
        val publisherPresenceStateFlow: MutableStateFlow<Boolean> = MutableStateFlow(false),
        val resolutions: MutableSharedFlow<Resolution> = MutableSharedFlow(replay = 1),
        val nextLocationUpdateIntervals: MutableSharedFlow<Long> = MutableSharedFlow(replay = 1),
    )
}
