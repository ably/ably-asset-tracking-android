package com.ably.tracking.subscriber

import com.ably.tracking.ConnectionException
import com.ably.tracking.ConnectionState
import com.ably.tracking.ConnectionStateChange
import com.ably.tracking.LocationUpdate
import com.ably.tracking.Resolution
import com.ably.tracking.TrackableState
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ClientTypes
import com.ably.tracking.common.PresenceAction
import com.ably.tracking.common.PresenceData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    val trackableStates: StateFlow<TrackableState>
}

internal fun createCoreSubscriber(
    ably: Ably,
    initialResolution: Resolution? = null,
    trackableId: String
): CoreSubscriber {
    return DefaultCoreSubscriber(ably, initialResolution, trackableId)
}

private class DefaultCoreSubscriber(
    private val ably: Ably,
    private val initialResolution: Resolution?,
    private val trackableId: String
) :
    CoreSubscriber {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val sendEventChannel: SendChannel<Event>
    private val _trackableStates: MutableStateFlow<TrackableState> = MutableStateFlow(TrackableState.Offline())
    private val _enhancedLocations: MutableSharedFlow<LocationUpdate> = MutableSharedFlow(replay = 1)

    override val enhancedLocations: SharedFlow<LocationUpdate>
        get() = _enhancedLocations.asSharedFlow()

    override val trackableStates: StateFlow<TrackableState>
        get() = _trackableStates.asStateFlow()

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
            // state
            val state = State()

            // processing
            for (event in receiveEventChannel) {
                // handle events after the subscriber is stopped
                if (state.isStopped) {
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
                        updateTrackableState(state)
                        ably.connect(trackableId, state.presenceData, useRewind = true) {
                            if (it.isSuccess) {
                                subscribeForEnhancedEvents()
                                try {
                                    subscribeForPresenceMessages()
                                } catch (exception: ConnectionException) {
                                    ably.disconnect(trackableId, state.presenceData) {
                                        event.handler(Result.failure(exception))
                                    }
                                    return@connect
                                }
                                subscribeForChannelState()
                            }
                            event.handler(it)
                        }
                    }
                    is PresenceMessageEvent -> {
                        when (event.presenceMessage.action) {
                            PresenceAction.PRESENT_OR_ENTER -> {
                                if (event.presenceMessage.data.type == ClientTypes.PUBLISHER) {
                                    state.isPublisherOnline = true
                                    updateTrackableState(state)
                                }
                            }
                            PresenceAction.LEAVE_OR_ABSENT -> {
                                if (event.presenceMessage.data.type == ClientTypes.PUBLISHER) {
                                    state.isPublisherOnline = false
                                    updateTrackableState(state)
                                }
                            }
                            else -> Unit
                        }
                    }
                    is ChangeResolutionEvent -> {
                        state.presenceData = state.presenceData.copy(resolution = event.resolution)
                        ably.updatePresenceData(trackableId, state.presenceData) {
                            event.handler(it)
                        }
                    }
                    is StopEvent -> {
                        try {
                            ably.close(state.presenceData)
                            state.isStopped = true
                            notifyAssetIsOffline()
                            event.handler(Result.success(Unit))
                        } catch (exception: ConnectionException) {
                            event.handler(Result.failure(exception))
                        }
                    }
                    is AblyConnectionStateChangeEvent -> {
                        state.lastConnectionStateChange = event.connectionStateChange
                        updateTrackableState(state)
                    }
                    is ChannelConnectionStateChangeEvent -> {
                        state.lastChannelConnectionStateChange = event.connectionStateChange
                        updateTrackableState(state)
                    }
                }
            }
        }
    }

    private fun updateTrackableState(state: State) {
        val newTrackableState = when (state.lastConnectionStateChange.state) {
            ConnectionState.ONLINE -> {
                when (state.lastChannelConnectionStateChange.state) {
                    ConnectionState.ONLINE -> if (state.isPublisherOnline) TrackableState.Online else TrackableState.Offline()
                    ConnectionState.OFFLINE -> TrackableState.Offline()
                    ConnectionState.FAILED -> TrackableState.Failed(state.lastChannelConnectionStateChange.errorInformation!!) // are we sure error information will always be present?
                }
            }
            ConnectionState.OFFLINE -> TrackableState.Offline()
            ConnectionState.FAILED -> TrackableState.Failed(state.lastConnectionStateChange.errorInformation!!) // are we sure error information will always be present?
        }
        if (newTrackableState != state.trackableState) {
            state.trackableState = newTrackableState
            scope.launch { _trackableStates.emit(newTrackableState) }
        }
    }

    private fun subscribeForChannelState() {
        ably.subscribeForChannelStateChange(trackableId) {
            enqueue(ChannelConnectionStateChangeEvent(it))
        }
    }

    private fun subscribeForPresenceMessages() {
        ably.subscribeForPresenceMessages(trackableId) {
            enqueue(PresenceMessageEvent(it))
        }
    }

    private fun subscribeForEnhancedEvents() {
        ably.subscribeForEnhancedEvents(trackableId) {
            scope.launch { _enhancedLocations.emit(it) }
        }
    }

    private fun notifyAssetIsOffline() {
        scope.launch { _trackableStates.emit(TrackableState.Offline()) }
    }

    private inner class State(
        var isStopped: Boolean = false,
        var isPublisherOnline: Boolean = false,
        var trackableState: TrackableState = TrackableState.Offline(),
        var lastConnectionStateChange: ConnectionStateChange = ConnectionStateChange(
            ConnectionState.OFFLINE, ConnectionState.OFFLINE, null
        ),
        var lastChannelConnectionStateChange: ConnectionStateChange = ConnectionStateChange(
            ConnectionState.OFFLINE, ConnectionState.OFFLINE, null
        ),
        var presenceData: PresenceData = PresenceData(ClientTypes.SUBSCRIBER, initialResolution)
    )
}
