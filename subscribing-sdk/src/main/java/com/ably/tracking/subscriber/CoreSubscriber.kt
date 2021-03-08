package com.ably.tracking.subscriber

import com.ably.tracking.AblyException
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
    fun request(request: Request)
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
    }

    override fun enqueue(event: AdhocEvent) {
        scope.launch { sendEventChannel.send(event) }
    }

    override fun request(request: Request) {
        scope.launch { sendEventChannel.send(request) }
    }

    private fun CoroutineScope.sequenceEventsQueue(receiveEventChannel: ReceiveChannel<Event>) {
        launch {
            var presenceData = PresenceData(ClientTypes.SUBSCRIBER, initialResolution)
            for (event in receiveEventChannel) {
                when (event) {
                    is StartEvent -> {
                        notifyAssetIsOffline()
                        subscribeForEnhancedEvents()
                        subscribeForPresenceMessages()
                        ably.connect(trackableId, presenceData, useRewind = true) {
                            // TODO what should we do when connection fails?
                        }
                    }
                    is PresenceMessageEvent -> {
                        when (event.presenceMessage.action) {
                            PresenceAction.PRESENT_OR_ENTER -> {
                                if (event.presenceMessage.data.type == ClientTypes.PUBLISHER) {
                                    notifyAssetIsOnline()
                                }
                            }
                            PresenceAction.LEAVE_OR_ABSENT -> {
                                if (event.presenceMessage.data.type == ClientTypes.PUBLISHER) {
                                    notifyAssetIsOffline()
                                }
                            }
                            else -> Unit
                        }
                    }
                    is ChangeResolutionEvent -> {
                        presenceData = presenceData.copy(resolution = event.resolution)
                        ably.updatePresenceData(trackableId, presenceData) {
                            event.handler(it)
                        }
                    }
                    is StopEvent -> {
                        notifyAssetIsOffline()
                        try {
                            ably.close(presenceData)
                            event.handler(Result.success(Unit))
                        } catch (exception: AblyException) {
                            event.handler(Result.failure(exception))
                        }
                    }
                }
            }
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

    private suspend fun notifyAssetIsOnline() {
        _trackableStates.emit(TrackableState.Online())
    }

    private suspend fun notifyAssetIsOffline() {
        _trackableStates.emit(TrackableState.Offline())
    }
}
