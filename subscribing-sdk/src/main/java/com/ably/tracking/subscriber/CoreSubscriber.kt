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
import com.ably.tracking.logging.LogHandler
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

/**
 * This interface exposes methods for [DefaultSubscriber].
 */
internal interface CoreSubscriber {
    fun enqueue(workerSpecification: WorkerSpecification)
    val enhancedLocations: SharedFlow<LocationUpdate>
    val rawLocations: SharedFlow<LocationUpdate>
    val trackableStates: StateFlow<TrackableState>
    val publisherPresence: StateFlow<Boolean>
    val resolutions: SharedFlow<Resolution>
    val nextLocationUpdateIntervals: SharedFlow<Long>
}

/**
 * This interface exposes methods for workers created by [WorkerFactory].
 */
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
    logHandler: LogHandler?,
): CoreSubscriber {
    return DefaultCoreSubscriber(ably, initialResolution, trackableId, logHandler)
}

/**
 * This is a private static single thread dispatcher that will be used for all the [Subscriber] instances.
 */
private val singleThreadDispatcher = createSingleThreadDispatcher()

private class DefaultCoreSubscriber(
    private val ably: Ably,
    initialResolution: Resolution?,
    private val trackableId: String,
    logHandler: LogHandler?,
) :
    CoreSubscriber, SubscriberInteractor {
    private val workerQueue: WorkerQueue<SubscriberProperties, WorkerSpecification>

    private val eventFlows: SubscriberProperties.EventFlows

    override val enhancedLocations: SharedFlow<LocationUpdate>
        get() = eventFlows.enhancedLocations

    override val rawLocations: SharedFlow<LocationUpdate>
        get() = eventFlows.rawLocations

    override val trackableStates: StateFlow<TrackableState>
        get() = eventFlows.trackableStates

    override val publisherPresence: StateFlow<Boolean>
        get() = eventFlows.publisherPresence

    override val resolutions: SharedFlow<Resolution>
        get() = eventFlows.resolutions

    override val nextLocationUpdateIntervals: SharedFlow<Long>
        get() = eventFlows.nextLocationUpdateIntervals

    init {
        val workerFactory = WorkerFactory(this, ably, trackableId)
        val scope = CoroutineScope(singleThreadDispatcher + SupervisorJob())
        eventFlows = SubscriberProperties.EventFlows(scope)
        val properties = SubscriberProperties(initialResolution, eventFlows)
        workerQueue = WorkerQueue(
            properties = properties,
            scope = scope,
            workerFactory = workerFactory,
            copyProperties = { copy() },
            getStoppedException = { SubscriberStoppedException() },
            logHandler = logHandler,
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
            eventFlows.emitEnhanced(it)
        }
    }

    override fun subscribeForRawEvents(presenceData: PresenceData) {
        ably.subscribeForRawEvents(trackableId, presenceData) {
            eventFlows.emitRaw(it)
        }
    }

    override fun notifyAssetIsOffline() {
        // TODO what is this method achieving, why is it not in normal flow?
        // Perhaps related to: https://github.com/ably/ably-asset-tracking-android/issues/802
        eventFlows.emit(TrackableState.Offline())
    }
}

internal data class SubscriberProperties private constructor(
    var presenceData: PresenceData,
    private val eventFlows: EventFlows,

    override var isStopped: Boolean = false,

    private var presentPublisherMemberKeys: MutableSet<String> = HashSet(),
    private var lastEmittedValueOfIsPublisherVisible: Boolean? = null,
    private var lastEmittedTrackableState: TrackableState = TrackableState.Offline(),
    private var lastConnectionStateChange: ConnectionStateChange =
        ConnectionStateChange(ConnectionState.OFFLINE, null),
    private var lastChannelConnectionStateChange: ConnectionStateChange =
        ConnectionStateChange(ConnectionState.OFFLINE, null),
    private var pendingPublisherResolutions: PendingResolutions = PendingResolutions(),
) : Properties {
    internal constructor(
        initialResolution: Resolution?,
        eventFlows: EventFlows,
    ) : this(PresenceData(ClientTypes.SUBSCRIBER, initialResolution), eventFlows)

    fun updateForConnectionStateChangeAndThenEmitStateEventsIfRequired(stateChange: ConnectionStateChange) {
        lastConnectionStateChange = stateChange
        emitStateEventsIfRequired()
    }

    fun updateForChannelConnectionStateChangeAndThenEmitStateEventsIfRequired(stateChange: ConnectionStateChange) {
        lastChannelConnectionStateChange = stateChange
        emitStateEventsIfRequired()
    }

    fun updateForPresenceMessagesAndThenEmitStateEventsIfRequired(presenceMessages: List<PresenceMessage>) {
        for (presenceMessage in presenceMessages) {
            // We are only interested in presence updates from publishers.
            if (presenceMessage.data.type == ClientTypes.PUBLISHER) {

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
        }
        emitStateEventsIfRequired()
    }

    fun emitStateEventsIfRequired() {
        val isAPublisherPresent = (presentPublisherMemberKeys.isNotEmpty())

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
            eventFlows.emit(trackableState)
        }

        // It is possible for presentPublisherMemberKeys to not be empty, even when we have no connectivity from our side,
        // because we've had presence entry events without subsequent leave events.
        // Therefore, from the perspective of a user consuming events from publisherPresenceStateFlow, what matters
        // is what we're computing for isPublisherVisible (not the simple isAPublisherPresent).
        val isPublisherVisible = (isAPublisherPresent && lastConnectionStateChange.state == ConnectionState.ONLINE)
        if (lastEmittedValueOfIsPublisherVisible != isPublisherVisible) {
            lastEmittedValueOfIsPublisherVisible = isPublisherVisible
            eventFlows.emitPublisherPresence(isPublisherVisible)
        }

        eventFlows.emit(pendingPublisherResolutions.drain())
    }

    internal class EventFlows(private val scope: CoroutineScope) {
        private val _enhancedLocations: MutableSharedFlow<LocationUpdate> = MutableSharedFlow(replay = 1)
        private val _rawLocations: MutableSharedFlow<LocationUpdate> = MutableSharedFlow(replay = 1)
        private val _trackableStates: MutableStateFlow<TrackableState> = MutableStateFlow(TrackableState.Offline())
        private val _publisherPresence: MutableStateFlow<Boolean> = MutableStateFlow(false)
        private val _resolutions: MutableSharedFlow<Resolution> = MutableSharedFlow(replay = 1)
        private val _nextLocationUpdateIntervals: MutableSharedFlow<Long> = MutableSharedFlow(replay = 1)

        fun emitEnhanced(locationUpdate: LocationUpdate) {
            scope.launch { _enhancedLocations.emit(locationUpdate) }
        }

        fun emitRaw(locationUpdate: LocationUpdate) {
            scope.launch { _rawLocations.emit(locationUpdate) }
        }

        fun emitPublisherPresence(isPublisherPresent: Boolean) {
            scope.launch { _publisherPresence.emit(isPublisherPresent) }
        }

        fun emit(trackableState: TrackableState) {
            scope.launch { _trackableStates.emit(trackableState) }
        }

        fun emit(resolutions: Array<Resolution>) {
            if (resolutions.isNotEmpty()) {
                scope.launch {
                    for (resolution in resolutions) {
                        _resolutions.emit(resolution)
                        _nextLocationUpdateIntervals.emit(resolution.desiredInterval)
                    }
                }
            }
        }

        val enhancedLocations: SharedFlow<LocationUpdate>
            get() = _enhancedLocations.asSharedFlow()

        val rawLocations: SharedFlow<LocationUpdate>
            get() = _rawLocations.asSharedFlow()

        val trackableStates: StateFlow<TrackableState>
            get() = _trackableStates.asStateFlow()

        val publisherPresence: StateFlow<Boolean>
            get() = _publisherPresence

        val resolutions: SharedFlow<Resolution>
            get() = _resolutions.asSharedFlow()

        val nextLocationUpdateIntervals: SharedFlow<Long>
            get() = _nextLocationUpdateIntervals.asSharedFlow()
    }

    private class PendingResolutions {
        private val resolutions: MutableList<Resolution> = ArrayList()

        fun add(resolution: Resolution) {
            resolutions.add(resolution)
        }

        fun drain(): Array<Resolution> {
            val array = resolutions.toTypedArray()
            resolutions.clear()
            return array
        }
    }
}
