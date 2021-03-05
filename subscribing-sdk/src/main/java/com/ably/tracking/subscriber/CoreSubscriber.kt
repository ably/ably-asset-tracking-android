package com.ably.tracking.subscriber

import com.ably.tracking.AssetState
import com.ably.tracking.LocationUpdate
import com.ably.tracking.Resolution
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
    val assetStates: StateFlow<AssetState>
}

internal fun createCoreSubscriber(
    ably: Ably,
    initialResolution: Resolution? = null
): CoreSubscriber {
    return DefaultCoreSubscriber(ably, initialResolution)
}

private class DefaultCoreSubscriber(
    private val ably: Ably,
    private val initialResolution: Resolution?
) :
    CoreSubscriber {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val sendEventChannel: SendChannel<Event>
    private val _assetStates: MutableStateFlow<AssetState> = MutableStateFlow(AssetState.Offline())
    private val _enhancedLocations: MutableSharedFlow<LocationUpdate> = MutableSharedFlow(replay = 1)

    override val enhancedLocations: SharedFlow<LocationUpdate>
        get() = _enhancedLocations.asSharedFlow()

    override val assetStates: StateFlow<AssetState>
        get() = _assetStates.asStateFlow()

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
                        ably.connect(presenceData) {
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
                        ably.updatePresenceData(presenceData) {
                            event.handler(it)
                        }
                    }
                    is StopEvent -> {
                        notifyAssetIsOffline()
                        ably.close(presenceData) {
                            event.handler(it)
                        }
                    }
                }
            }
        }
    }

    private fun subscribeForPresenceMessages() {
        ably.subscribeForPresenceMessages {
            enqueue(PresenceMessageEvent(it))
        }
    }

    private fun subscribeForEnhancedEvents() {
        ably.subscribeForEnhancedEvents {
            scope.launch { _enhancedLocations.emit(it) }
        }
    }

    private suspend fun notifyAssetIsOnline() {
        _assetStates.emit(AssetState.Online())
    }

    private suspend fun notifyAssetIsOffline() {
        _assetStates.emit(AssetState.Offline())
    }
}
