package com.ably.tracking.publisher

import android.Manifest
import androidx.annotation.RequiresPermission
import com.ably.tracking.common.ClientTypes
import com.ably.tracking.common.PresenceData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

internal interface CorePublisher {
    fun enqueue(event: AdhocEvent)
    fun request(request: Request)
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
