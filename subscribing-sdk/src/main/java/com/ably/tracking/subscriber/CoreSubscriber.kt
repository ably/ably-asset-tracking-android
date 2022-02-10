package com.ably.tracking.subscriber

import com.ably.tracking.ConnectionException
import com.ably.tracking.LocationUpdate
import com.ably.tracking.Resolution
import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ClientTypes
import com.ably.tracking.common.ConnectionState
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.PresenceAction
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.createSingleThreadDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal interface CoreSubscriber {
    fun enqueue(event: AdhocEvent)
    fun request(request: Request<*>)
    val enhancedLocations: SharedFlow<LocationUpdate>
    val rawLocations: SharedFlow<LocationUpdate>
    val trackableStates: StateFlow<TrackableState>
    val resolutions: SharedFlow<Resolution>
    val nextLocationUpdateIntervals: SharedFlow<Long>
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
    private val initialResolution: Resolution?,
    private val trackableId: String,
) :
    CoreSubscriber {
    private val scope = CoroutineScope(singleThreadDispatcher + SupervisorJob())
    private val sendEventChannel: SendChannel<Event>
    private val _trackableStates: MutableStateFlow<TrackableState> = MutableStateFlow(TrackableState.Offline())
    private val _enhancedLocations: MutableSharedFlow<LocationUpdate> = MutableSharedFlow(replay = 1)
    private val _rawLocations: MutableSharedFlow<LocationUpdate> = MutableSharedFlow(replay = 1)
    private val _resolutions: MutableSharedFlow<Resolution> = MutableSharedFlow(replay = 1)
    private val _nextLocationUpdateIntervals: MutableSharedFlow<Long> = MutableSharedFlow(replay = 1)

    override val enhancedLocations: SharedFlow<LocationUpdate>
        get() = _enhancedLocations.asSharedFlow()

    override val rawLocations: SharedFlow<LocationUpdate>
        get() = _rawLocations.asSharedFlow()

    override val trackableStates: StateFlow<TrackableState>
        get() = _trackableStates.asStateFlow()

    override val resolutions: SharedFlow<Resolution>
        get() = _resolutions.asSharedFlow()

    override val nextLocationUpdateIntervals: SharedFlow<Long>
        get() = _nextLocationUpdateIntervals.asSharedFlow()

    init {
        val channel = Channel<Event>()
        sendEventChannel = channel
        scope.launch {
            coroutineScope {
                sequenceEventsQueue(channel)
            }
        }
        ably.subscribeForAblyStateChange { enqueue(AblyConnectionStateChangeEvent(it)) }
    }

    override fun enqueue(event: AdhocEvent) {
        scope.launch { sendEventChannel.send(event) }
    }

    override fun request(request: Request<*>) {
        scope.launch { sendEventChannel.send(request) }
    }

    private fun CoroutineScope.sequenceEventsQueue(receiveEventChannel: ReceiveChannel<Event>) {
        launch {
            val properties = Properties()

            // processing
            for (event in receiveEventChannel) {
                // handle events after the subscriber is stopped
                if (properties.isStopped) {
                    if (event is Request<*>) {
                        // when the event is a request then call its handler
                        when (event) {
                            is StopEvent -> event.handler(Result.success(Unit))
                            else -> event.handler(Result.failure(SubscriberStoppedException()))
                        }
                        continue
                    } else if (event is AdhocEvent) {
                        // when the event is an adhoc event then just ignore it
                        continue
                    }
                }
                when (event) {
                    is StartEvent -> {
                        updateTrackableState(properties)
                        ably.connect(trackableId, properties.presenceData, useRewind = true, willSubscribe = true) {
                            if (it.isSuccess) {
                                request(ConnectionCreatedEvent(event.handler))
                            } else {
                                event.handler(it)
                            }
                        }
                    }
                    is ConnectionCreatedEvent -> {
                        ably.subscribeForPresenceMessages(
                            trackableId = trackableId,
                            listener = { enqueue(PresenceMessageEvent(it)) },
                            callback = { subscribeResult ->
                                if (subscribeResult.isSuccess) {
                                    request(ConnectionReadyEvent(event.handler))
                                } else {
                                    ably.disconnect(trackableId, properties.presenceData) {
                                        event.handler(subscribeResult)
                                    }
                                }
                            }
                        )
                    }
                    is ConnectionReadyEvent -> {
                        subscribeForChannelState()
                        subscribeForEnhancedEvents()
                        subscribeForRawEvents()
                        subscribeForResolutionEvents()
                        event.handler(Result.success(Unit))
                    }
                    is PresenceMessageEvent -> {
                        when (event.presenceMessage.action) {
                            PresenceAction.PRESENT_OR_ENTER -> {
                                if (event.presenceMessage.data.type == ClientTypes.PUBLISHER) {
                                    properties.isPublisherOnline = true
                                    updateTrackableState(properties)
                                }
                            }
                            PresenceAction.LEAVE_OR_ABSENT -> {
                                if (event.presenceMessage.data.type == ClientTypes.PUBLISHER) {
                                    properties.isPublisherOnline = false
                                    updateTrackableState(properties)
                                }
                            }
                            else -> Unit
                        }
                    }
                    is ChangeResolutionEvent -> {
                        properties.presenceData = properties.presenceData.copy(resolution = event.resolution)
                        ably.updatePresenceData(trackableId, properties.presenceData) {
                            event.handler(it)
                        }
                    }
                    is StopEvent -> {
                        try {
                            ably.close(properties.presenceData)
                            properties.isStopped = true
                            notifyAssetIsOffline()
                            event.handler(Result.success(Unit))
                        } catch (exception: ConnectionException) {
                            event.handler(Result.failure(exception))
                        }
                    }
                    is AblyConnectionStateChangeEvent -> {
                        properties.lastConnectionStateChange = event.connectionStateChange
                        updateTrackableState(properties)
                    }
                    is ChannelConnectionStateChangeEvent -> {
                        properties.lastChannelConnectionStateChange = event.connectionStateChange
                        updateTrackableState(properties)
                    }
                }
            }
        }
    }

    private fun updateTrackableState(properties: Properties) {
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

    private fun subscribeForChannelState() {
        ably.subscribeForChannelStateChange(trackableId) {
            enqueue(ChannelConnectionStateChangeEvent(it))
        }
    }

    private fun subscribeForEnhancedEvents() {
        ably.subscribeForEnhancedEvents(trackableId) {
            scope.launch { _enhancedLocations.emit(it) }
        }
    }

    private fun subscribeForRawEvents() {
        ably.subscribeForRawEvents(trackableId) {
            scope.launch { _rawLocations.emit(it) }
        }
    }

    private fun subscribeForResolutionEvents() {
        ably.subscribeForResolutionEvents(trackableId) {
            scope.launch { _resolutions.emit(it) }
            scope.launch { _nextLocationUpdateIntervals.emit(it.desiredInterval) }
        }
    }

    private fun notifyAssetIsOffline() {
        scope.launch { _trackableStates.emit(TrackableState.Offline()) }
    }

    private inner class Properties(
        var isStopped: Boolean = false,
        var isPublisherOnline: Boolean = false,
        var trackableState: TrackableState = TrackableState.Offline(),
        var lastConnectionStateChange: ConnectionStateChange = ConnectionStateChange(
            ConnectionState.OFFLINE, null
        ),
        var lastChannelConnectionStateChange: ConnectionStateChange = ConnectionStateChange(
            ConnectionState.OFFLINE, null
        ),
        var presenceData: PresenceData = PresenceData(ClientTypes.SUBSCRIBER, initialResolution)
    )
}
