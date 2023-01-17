package com.ably.tracking.publisher.debug

import com.ably.tracking.Location
import com.ably.tracking.logging.LogHandler
import com.mapbox.android.core.location.LocationEngineResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class FlowLocationEngine(
    flow: Flow<Location>,
    logHandler: LogHandler?,
) : BaseLocationEngine(logHandler) {
    init {
        flow.onEach {
            onLocationEngineResult(LocationEngineResult.create(it.toAndroid()))
        }.launchIn(CoroutineScope(Dispatchers.IO))
    }
}
