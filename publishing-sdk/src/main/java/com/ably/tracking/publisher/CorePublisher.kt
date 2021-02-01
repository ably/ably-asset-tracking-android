package com.ably.tracking.publisher

import android.Manifest
import androidx.annotation.RequiresPermission
import com.ably.tracking.ConnectionStateChange
import com.ably.tracking.common.ClientTypes
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
import timber.log.Timber

internal interface CorePublisher {
    fun enqueue(event: AdhocEvent)
    fun request(request: Request)
    val connectionStates: SharedFlow<ConnectionStateChange>
}

@RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
internal fun createCorePublisher(
    ablyService: AblyService,
    mapboxService: MapboxService
): CorePublisher {
    return DefaultCorePublisher(ablyService, mapboxService)
}

private class DefaultCorePublisher
@RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
constructor(
    private val ablyService: AblyService,
    private val mapboxService: MapboxService
) : CorePublisher {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val sendEventChannel: SendChannel<Event>
    private val _connectionStates = MutableSharedFlow<ConnectionStateChange>()
    override val connectionStates: SharedFlow<ConnectionStateChange>
        get() = _connectionStates.asSharedFlow()

    init {
        val channel = Channel<Event>()
        sendEventChannel = channel
        scope.launch {
            coroutineScope {
                sequenceEventsQueue(channel)
            }
        }
        ablyService.subscribeForAblyStateChange { state -> scope.launch { _connectionStates.emit(state) } }
    }

    override fun enqueue(event: AdhocEvent) {
        scope.launch { sendEventChannel.send(event) }
    }

    override fun request(request: Request) {
        scope.launch { sendEventChannel.send(request) }
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    private fun CoroutineScope.sequenceEventsQueue(receiveEventChannel: ReceiveChannel<Event>) {
        launch {
            // state
            var isTracking = false
            val presenceData = PresenceData(ClientTypes.PUBLISHER)

            // processing
            for (event in receiveEventChannel) {
                when (event) {
                    is StartEvent -> {
                        if (!isTracking) {
                            isTracking = true

                            Timber.e("startLocationUpdates")

                            mapboxService.startTrip()
                        }
                    }
                }
            }
        }
    }
}
