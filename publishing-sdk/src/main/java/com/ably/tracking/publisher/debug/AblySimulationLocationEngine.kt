package com.ably.tracking.publisher.debug

import android.os.SystemClock
import com.ably.tracking.common.EventNames
import com.ably.tracking.common.message.getLocationMessages
import com.ably.tracking.common.logging.d
import com.ably.tracking.common.logging.i
import com.ably.tracking.common.message.synopsis
import com.ably.tracking.common.message.toTracking
import com.ably.tracking.logging.LogHandler
import com.google.gson.Gson
import com.mapbox.android.core.location.LocationEngineResult
import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.types.ClientOptions

internal class AblySimulationLocationEngine(
    ablyOptions: ClientOptions,
    simulationTrackingId: String,
    logHandler: LogHandler?,
) :
    BaseLocationEngine(logHandler) {
    private val gson = Gson()

    init {
        val ably = AblyRealtime(ablyOptions)
        val simulationChannel = ably.channels.get(simulationTrackingId)

        ably.connection.on { logHandler?.i("Ably connection state change: $it") }
        simulationChannel.on { logHandler?.i("Ably channel state change: $it") }

        simulationChannel.subscribe(EventNames.ENHANCED) { message ->
            logHandler?.i("Ably channel message: $message")
            message.getLocationMessages(gson).forEach {
                logHandler?.d("Received enhanced location: ${it.synopsis()}")
                val loc = it.toTracking().toAndroid()
                loc.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                onLocationEngineResult(LocationEngineResult.create(loc))
            }
        }
    }
}
