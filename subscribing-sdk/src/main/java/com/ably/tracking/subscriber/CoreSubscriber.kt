package com.ably.tracking.subscriber

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
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

internal interface CoreSubscriberContract {
    fun enqueue(event: AdhocEvent)
    fun request(request: Request)
    val enhancedLocations: SharedFlow<LocationUpdate>
    val assetStatuses: SharedFlow<Boolean>
}

internal fun createCoreSubscriber(
    ablyService: AblyService,
    initialResolution: Resolution? = null
): CoreSubscriberContract {
    return CoreSubscriber(ablyService, initialResolution)
}

private class CoreSubscriber(
    private val ablyService: AblyService,
    private val initialResolution: Resolution?
) :
    CoreSubscriberContract {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val sendEventChannel: SendChannel<Event>
    private val _assetStatuses: MutableSharedFlow<Boolean> = MutableSharedFlow()
    private val _enhancedLocations: MutableSharedFlow<LocationUpdate> = MutableSharedFlow()

    override val enhancedLocations: SharedFlow<LocationUpdate>
        get() = _enhancedLocations.asSharedFlow()

    override val assetStatuses: SharedFlow<Boolean>
        get() = _assetStatuses.asSharedFlow()

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
                        // TODO - listen for the response
                        ablyService.connect(presenceData)
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
                        ablyService.updatePresenceData(presenceData) {
                            event.handler(it)
                        }
                    }
                    is StopEvent -> {
                        ablyService.close(presenceData)
                        notifyAssetIsOffline()
                        // TODO add proper handling for callback when stopping the subscriber (handle success and failure)
                        event.handler(Result.success(Unit))
                    }
                }
            }
        }
    }

    private fun subscribeForPresenceMessages() {
        ablyService.subscribeForPresenceMessages {
            enqueue(PresenceMessageEvent(it))
        }
    }

    private fun subscribeForEnhancedEvents() {
        ablyService.subscribeForEnhancedEvents {
            scope.launch { _enhancedLocations.emit(it) }
        }
    }

    private suspend fun notifyAssetIsOnline() {
        _assetStatuses.emit(true)
    }

    private suspend fun notifyAssetIsOffline() {
        _assetStatuses.emit(false)
    }
}
