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
    @Deprecated(
        "The publisherPresenceStateChanges SharedFlow provides more granular information on publisher presence. The Boolean version may be removed in a later version of AAT",
        replaceWith = ReplaceWith("publisherPresenceStateChanges")
    )
    val publisherPresence: StateFlow<Boolean>
    val publisherPresenceStateChanges: StateFlow<PublisherPresenceStateChange>
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

    @Deprecated(
        "The publisherPresenceStateChanges SharedFlow provides more granular information on publisher presence. The Boolean version may be removed in a later version of AAT",
        replaceWith = ReplaceWith("publisherPresenceStateChanges")
    )
    override val publisherPresence: StateFlow<Boolean>
        get() = eventFlows.publisherPresence

    override val publisherPresenceStateChanges: StateFlow<PublisherPresenceStateChange>
        get() = eventFlows.publisherPresenceStateChanges

    override val resolutions: SharedFlow<Resolution>
        get() = eventFlows.resolutions

    override val nextLocationUpdateIntervals: SharedFlow<Long>
        get() = eventFlows.nextLocationUpdateIntervals

    init {
        val workerFactory = WorkerFactory(this, ably, trackableId)
        val scope = CoroutineScope(singleThreadDispatcher + SupervisorJob())
        eventFlows = SubscriberProperties.EventFlows(scope, DefaultPublisherPresence(DefaultPublisherPresenceMessageProcessor(), scope))
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
            enqueue(WorkerSpecification.FetchHistoryForChannelConnectionStateChange(trackableId, it))
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
    private val updatingResolutions: MutableMap<String, MutableList<Resolution?>>,
    private val eventFlows: EventFlows,

    override var isStopped: Boolean = false,

    private var presentPublisherMemberKeys: MutableSet<String> = HashSet(),
    private var lastEmittedValueOfIsPublisherVisible: Boolean? = null,
    private var lastEmittedValueOfPublisherPresenceState: PublisherPresenceState? = null,
    private var lastEmittedTrackableState: TrackableState = TrackableState.Offline(),
    private var lastConnectionStateChange: ConnectionStateChange =
        ConnectionStateChange(ConnectionState.OFFLINE, null),
    private var lastChannelConnectionStateChange: ConnectionStateChange =
        ConnectionStateChange(ConnectionState.OFFLINE, null),
    private var pendingPublisherResolutions: PendingResolutions = PendingResolutions(),
    private var cachedRealtimePresenceMessages: MutableList<PresenceMessage> = mutableListOf()
) : Properties {
    internal constructor(
        initialResolution: Resolution?,
        eventFlows: EventFlows,
    ) : this(PresenceData(ClientTypes.SUBSCRIBER, initialResolution), mutableMapOf(), eventFlows)

    fun addUpdatingResolution(trackableId: String, resolution: Resolution?) {
        val updatingList = updatingResolutions[trackableId] ?: mutableListOf()
        updatingList.add(resolution)
        updatingResolutions[trackableId] = updatingList
    }

    fun containsUpdatingResolution(trackableId: String, resolution: Resolution?) =
        updatingResolutions[trackableId]?.contains(resolution) == true

    fun isLastUpdatingResolution(trackableId: String, resolution: Resolution?) =
        updatingResolutions[trackableId]?.last() == resolution

    fun removeUpdatingResolution(trackableId: String, resolution: Resolution?) {
        updatingResolutions[trackableId]?.remove(resolution)
        if (updatingResolutions[trackableId]?.isEmpty() == true) {
            updatingResolutions.remove(trackableId)
        }
    }

    fun updateForConnectionStateChangeAndThenEmitStateEventsIfRequired(stateChange: ConnectionStateChange) {
        /**
         * If a connection drops offline, then let the publisher presence handler know that this has happened.
         *
         * When it comes back online, there's no guarantee that the channel has re-attached yet, or that presence has
         * been re-entered, so handle that operation in the [updateForChannelConnectionStateChangeAndThenEmitStateEventsIfRequired]
         * handler when the channel attaches.
         */
        if (stateChange.state == ConnectionState.OFFLINE && !eventFlows.lastPublisherPresenceIsUnknown()) {
            eventFlows.emitPublisherPresenceUnknown()
        }

        lastConnectionStateChange = stateChange
        emitStateEventsIfRequired()
    }

    fun updateForChannelConnectionStateChangeAndThenEmitStateEventsIfRequired(
        stateChange: ConnectionStateChange,
        presenceHistory: List<PresenceMessage>?
    ) {
        /**
         * If the channel detaches, then let the publisher presence handlers know that the connection is no longer online. Note, that
         * per RTL3e, the overall connection dropping will not set the channel to detached (or in AAT, an offline state). The channel
         * only enters an OFFLINE state (in AAT terms) when the connection comes back, at which point the channel briefly goes back to
         * ATTACHING before returning to ATTACHED.
         *
         * If the channel has returned to the ATTACHED state (or ONLINE, in AAT terms), then we have the presence history for the channel plus
         * any realtime presence events received in the interim - let the publisher presence handlers know this.
         */
        if (stateChange.state == ConnectionState.OFFLINE && !eventFlows.lastPublisherPresenceIsUnknown()) {
            eventFlows.emitPublisherPresenceUnknown()
        } else {
            eventFlows.emitPublisherPresenceStateChange((presenceHistory ?: mutableListOf()) + cachedRealtimePresenceMessages)
            cachedRealtimePresenceMessages.clear()
        }

        lastChannelConnectionStateChange = stateChange
        emitStateEventsIfRequired()
    }

    fun updateForPresenceMessagesAndThenEmitStateEventsIfRequired(presenceMessages: List<PresenceMessage>) {
        /*
            For the extended publisher presence API, we will need to amalgamate any messages received on realtime
            with the presence history for a channel, and ideally this needs to happen in one go when the channel
            comes online, not in drips and drabs.

            So, if the channel is in an offline state (aka, we're still fetching the presence history before reporting the
            channel as online again), cache and received messages.
         */
        if (eventFlows.lastPublisherPresenceIsUnknown()) {
            cachedRealtimePresenceMessages += presenceMessages
        } else {
            eventFlows.emitPublisherPresenceStateChange(presenceMessages)
        }

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

    internal class EventFlows(
        private val scope: CoroutineScope,
        private val publisherPresenceMonitor: PublisherPresence
    ) {
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

        fun emitPublisherPresenceUnknown() {
            publisherPresenceMonitor.connectionOffline()
        }

        fun emitPublisherPresenceStateChange(presenceMessages: List<PresenceMessage>) {
            publisherPresenceMonitor.processPresenceMessages(presenceMessages)
        }

        fun lastPublisherPresenceIsUnknown(): Boolean = publisherPresenceMonitor.lastStateIsUnknown()

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

        val publisherPresenceStateChanges: StateFlow<PublisherPresenceStateChange>
            get() = publisherPresenceMonitor.stateChanges

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
