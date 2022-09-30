package com.ably.tracking.subscriber

import com.ably.tracking.LocationUpdate
import com.ably.tracking.Resolution
import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ClientTypes
import com.ably.tracking.common.ConnectionState
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.createSingleThreadDispatcher
import com.ably.tracking.subscriber.workerqueue.WorkerFactory
import com.ably.tracking.subscriber.workerqueue.WorkerSpecification
import com.ably.tracking.subscriber.workerqueue.WorkerQueue
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
    fun updateTrackableState(properties: Properties)
    fun updatePublisherPresence(properties: Properties, isPublisherPresent: Boolean)
    fun updatePublisherResolutionInformation(presenceData: PresenceData)
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
    private val scope = CoroutineScope(singleThreadDispatcher + SupervisorJob())
    private val workerQueue: WorkerQueue
    private val _trackableStates: MutableStateFlow<TrackableState> =
        MutableStateFlow(TrackableState.Offline())
    private val _publisherPresence: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val _enhancedLocations: MutableSharedFlow<LocationUpdate> =
        MutableSharedFlow(replay = 1)
    private val _rawLocations: MutableSharedFlow<LocationUpdate> = MutableSharedFlow(replay = 1)
    private val _resolutions: MutableSharedFlow<Resolution> = MutableSharedFlow(replay = 1)
    private val _nextLocationUpdateIntervals: MutableSharedFlow<Long> =
        MutableSharedFlow(replay = 1)

    override val enhancedLocations: SharedFlow<LocationUpdate>
        get() = _enhancedLocations.asSharedFlow()

    override val rawLocations: SharedFlow<LocationUpdate>
        get() = _rawLocations.asSharedFlow()

    override val trackableStates: StateFlow<TrackableState>
        get() = _trackableStates.asStateFlow()

    override val publisherPresence: StateFlow<Boolean>
        get() = _publisherPresence

    override val resolutions: SharedFlow<Resolution>
        get() = _resolutions.asSharedFlow()

    override val nextLocationUpdateIntervals: SharedFlow<Long>
        get() = _nextLocationUpdateIntervals.asSharedFlow()

    init {
        val workerFactory = WorkerFactory(this, ably, trackableId)
        val properties = Properties(initialResolution)
        workerQueue = WorkerQueue(properties, scope, workerFactory)

        ably.subscribeForAblyStateChange { enqueue(WorkerSpecification.UpdateConnectionState(it)) }
    }

    override fun enqueue(workerSpecification: WorkerSpecification) {
        workerQueue.enqueue(workerSpecification)
    }

    override fun updatePublisherPresence(properties: Properties, isPublisherPresent: Boolean) {
        if (isPublisherPresent != properties.isPublisherOnline) {
            properties.isPublisherOnline = isPublisherPresent
            scope.launch { _publisherPresence.emit(isPublisherPresent) }
        }
    }

    override fun updateTrackableState(properties: Properties) {
        val newTrackableState = when (properties.lastConnectionStateChange.state) {
            ConnectionState.ONLINE -> {
                when (properties.lastChannelConnectionStateChange.state) {
                    ConnectionState.ONLINE -> if (properties.isPublisherOnline) TrackableState.Online else TrackableState.Offline()
                    ConnectionState.OFFLINE -> TrackableState.Offline()
                    ConnectionState.FAILED -> TrackableState.Failed(properties.lastChannelConnectionStateChange.errorInformation!!) // are we sure error information will always be present?
                }
            }
            ConnectionState.OFFLINE -> TrackableState.Offline()
            ConnectionState.FAILED -> TrackableState.Failed(properties.lastConnectionStateChange.errorInformation!!) // are we sure error information will always be present?
        }
        if (newTrackableState != properties.trackableState) {
            properties.trackableState = newTrackableState
            scope.launch { _trackableStates.emit(newTrackableState) }
        }
    }

    override fun subscribeForChannelState() {
        ably.subscribeForChannelStateChange(trackableId) {
            enqueue(WorkerSpecification.UpdateChannelConnectionState(it))
        }
    }

    override fun subscribeForEnhancedEvents(presenceData: PresenceData) {
        ably.subscribeForEnhancedEvents(trackableId, presenceData) {
            scope.launch { _enhancedLocations.emit(it) }
        }
    }

    override fun subscribeForRawEvents(presenceData: PresenceData) {
        ably.subscribeForRawEvents(trackableId, presenceData) {
            scope.launch { _rawLocations.emit(it) }
        }
    }

    override fun updatePublisherResolutionInformation(presenceData: PresenceData) {
        presenceData.resolution?.let { publisherResolution ->
            scope.launch { _resolutions.emit(publisherResolution) }
            scope.launch { _nextLocationUpdateIntervals.emit(publisherResolution.desiredInterval) }
        }
    }

    override fun notifyAssetIsOffline() {
        scope.launch { _trackableStates.emit(TrackableState.Offline()) }
    }
}

internal class Properties(initialResolution: Resolution?) {
    var isStopped: Boolean = false
    var isPublisherOnline: Boolean = false
    var trackableState: TrackableState = TrackableState.Offline()
    var lastConnectionStateChange: ConnectionStateChange =
        ConnectionStateChange(ConnectionState.OFFLINE, null)
    var lastChannelConnectionStateChange: ConnectionStateChange =
        ConnectionStateChange(ConnectionState.OFFLINE, null)
    var presenceData: PresenceData = PresenceData(ClientTypes.SUBSCRIBER, initialResolution)
}
