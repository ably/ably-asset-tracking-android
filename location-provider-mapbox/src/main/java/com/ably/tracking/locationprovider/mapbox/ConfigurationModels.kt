package com.ably.tracking.locationprovider.mapbox

import android.app.Notification

data class MapConfiguration(val apiKey: String)

/**
 * Provides the notification that will be used for the background tracking service.
 */
interface PublisherNotificationProvider {
    /**
     * Returns the notification that will be displayed. This method can be called multiple times.
     */
    fun getNotification(): Notification
}

sealed class LocationSource
class LocationSourceAbly private constructor(val simulationChannelName: String) : LocationSource() {
    companion object {
        @JvmStatic
        fun create(simulationChannelName: String) = LocationSourceAbly(simulationChannelName)
    }

    private constructor() : this("")
}

class LocationSourceRaw private constructor(
    val historyData: MapboxLocationHistoryData,
    val onDataEnded: (() -> Unit)? = null
) :
    LocationSource() {
    companion object {
        @JvmSynthetic
        fun create(historyData: MapboxLocationHistoryData, onDataEnded: (() -> Unit)? = null) =
            LocationSourceRaw(historyData, onDataEnded)

        @JvmStatic
        fun createRaw(historyData: MapboxLocationHistoryData, callback: (DataEndedCallback)? = null) =
            LocationSourceRaw(historyData, callback)
    }

    private constructor() : this(historyData = MapboxLocationHistoryData(emptyList()), onDataEnded = null)
    private constructor(historyData: MapboxLocationHistoryData, callback: (DataEndedCallback)? = null) : this(
        historyData,
        { callback?.onDataEnded() }
    )
}

interface DataEndedCallback {
    fun onDataEnded()
}
