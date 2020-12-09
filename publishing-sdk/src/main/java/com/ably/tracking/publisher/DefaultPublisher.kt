package com.ably.tracking.publisher

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresPermission
import com.ably.tracking.ConnectionConfiguration
import com.ably.tracking.common.ClientTypes
import com.ably.tracking.common.EventNames
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.toGeoJson
import com.ably.tracking.common.toJsonArray
import com.ably.tracking.publisher.debug.AblySimulationLocationEngine
import com.google.gson.Gson
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.core.replay.history.ReplayHistoryMapper
import com.mapbox.navigation.core.trip.session.LocationObserver
import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.realtime.Channel
import io.ably.lib.realtime.CompletionListener
import io.ably.lib.types.AblyException
import io.ably.lib.types.ClientOptions
import io.ably.lib.types.ErrorInfo
import timber.log.Timber

@SuppressLint("LogConditional")
internal class DefaultPublisher
@RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
constructor(
    private val connectionConfiguration: ConnectionConfiguration,
    mapConfiguration: MapConfiguration,
    private val debugConfiguration: DebugConfiguration?,
    private val locationUpdatedListener: LocationUpdatedListener,
    context: Context
) :
    Publisher {
    private val gson: Gson = Gson()
    private val mapboxNavigation: MapboxNavigation
    private val ably: AblyRealtime
    private val channelMap: MutableMap<String, Channel> = mutableMapOf()
    private val locationObserver = object : LocationObserver {
        override fun onRawLocationChanged(rawLocation: Location) {
            sendRawLocationMessage(rawLocation)
        }

        override fun onEnhancedLocationChanged(
            enhancedLocation: Location,
            keyPoints: List<Location>
        ) {
            sendEnhancedLocationMessage(enhancedLocation, keyPoints)
        }
    }
    private val presenceData = PresenceData(ClientTypes.PUBLISHER)
    private var isTracking: Boolean = false
    private var mapboxReplayer: MapboxReplayer? = null

    init {
        ably = AblyRealtime(connectionConfiguration.apiKey)

        Timber.w("Started.")

        val mapboxBuilder = MapboxNavigation.defaultNavigationOptionsBuilder(
            context,
            mapConfiguration.apiKey
        )
        debugConfiguration?.locationSource?.let { locationSource ->
            when (locationSource) {
                is LocationSourceAbly -> {
                    useAblySimulationLocationEngine(mapboxBuilder, locationSource)
                }
                is LocationSourceRaw -> {
                    useHistoryDataReplayerLocationEngine(mapboxBuilder, locationSource)
                }
            }
        }

        debugConfiguration?.ablyStateChangeListener?.let { ablyStateChangeListener ->
            ably.connection.on { state -> postToMainThread { ablyStateChangeListener(state) } }
        }

        mapboxNavigation = MapboxNavigation(mapboxBuilder.build())
        mapboxNavigation.registerLocationObserver(locationObserver)
        startLocationUpdates()
    }

    private fun useAblySimulationLocationEngine(
        mapboxBuilder: NavigationOptions.Builder,
        locationSource: LocationSourceAbly
    ) {
        mapboxBuilder.locationEngine(
            AblySimulationLocationEngine(
                ClientOptions(connectionConfiguration.apiKey),
                locationSource.simulationChannelName
            )
        )
    }

    private fun useHistoryDataReplayerLocationEngine(
        mapboxBuilder: NavigationOptions.Builder,
        locationSource: LocationSourceRaw
    ) {
        mapboxReplayer = MapboxReplayer().apply {
            mapboxBuilder.locationEngine(ReplayLocationEngine(this))
            this.clearEvents()
            this.pushEvents(ReplayHistoryMapper().mapToReplayEvents(locationSource.historyData))
            this.play()
        }
    }

    private fun sendRawLocationMessage(rawLocation: Location) {
        val geoJsonMessage = rawLocation.toGeoJson()
        Timber.d("sendRawLocationMessage: publishing: ${geoJsonMessage.synopsis()}")
        channelMap.values.forEach {
            it.publish(EventNames.RAW, geoJsonMessage.toJsonArray(gson))
        }
        locationUpdatedListener(rawLocation)
    }

    private fun sendEnhancedLocationMessage(enhancedLocation: Location, keyPoints: List<Location>) {
        val locations = if (keyPoints.isEmpty()) listOf(enhancedLocation) else keyPoints
        val geoJsonMessages = locations.map { it.toGeoJson() }
        geoJsonMessages.forEach {
            Timber.d("sendEnhancedLocationMessage: publishing: ${it.synopsis()}")
        }
        channelMap.values.forEach {
            it.publish(EventNames.ENHANCED, geoJsonMessages.toJsonArray(gson))
        }
        locationUpdatedListener(enhancedLocation)
    }

    // TODO define threading strategy: https://github.com/ably/ably-asset-tracking-android/issues/22
    @RequiresPermission(anyOf = [ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION])
    @Synchronized
    private fun startLocationUpdates() {
        if (!isTracking) {
            isTracking = true

            Timber.e("startLocationUpdates")

            mapboxNavigation.apply {
                toggleHistory(true)
                startTripSession()
            }
        }
    }

    override fun track(trackable: Trackable, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        if (this.active != null) {
            throw IllegalStateException("For this preview version of the SDK, this method may only be called once for any given instance of this class.")
        }

        add(
            trackable,
            {
                active = trackable
                onSuccess()
            },
            onError
        )
    }

    override fun add(trackable: Trackable, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        if (!channelMap.contains(trackable.id)) {
            createChannelAndJoinPresence(
                trackable,
                {
                    channelMap[trackable.id] = it
                    onSuccess()
                },
                onError
            )
        } else {
            onSuccess()
        }
    }

    override fun remove(
        trackable: Trackable,
        onSuccess: (wasPresent: Boolean) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val removedChannel = channelMap.remove(trackable.id)
        if (removedChannel != null) {
            leaveChannelPresence(
                removedChannel,
                {
                    if (active == trackable) {
                        active = null
                    }
                    onSuccess(true)
                },
                onError
            )
        } else {
            onSuccess(false)
        }
    }

    override var active: Trackable? = null

    private fun createChannelAndJoinPresence(
        trackable: Trackable,
        onSuccess: (Channel) -> Unit,
        onError: (Exception) -> Unit
    ) {
        ably.channels.get(trackable.id).apply {
            try {
                presence.enterClient(
                    connectionConfiguration.clientId,
                    gson.toJson(presenceData),
                    object : CompletionListener {
                        override fun onSuccess() {
                            onSuccess(this@apply)
                        }

                        override fun onError(reason: ErrorInfo?) {
                            Timber.e("Unable to enter presence: ${reason?.message}")
                            onError(Exception("Unable to enter presence: ${reason?.message}"))
                        }
                    }
                )
            } catch (ablyException: AblyException) {
                Timber.e(ablyException)
                onError(ablyException)
            }
        }
    }

    private fun leaveChannelPresence(
        channel: Channel,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        try {
            channel.presence.leaveClient(
                connectionConfiguration.clientId,
                gson.toJson(presenceData),
                object : CompletionListener {
                    override fun onSuccess() {
                        onSuccess()
                    }

                    override fun onError(reason: ErrorInfo?) {
                        Timber.e("Unable to leave presence: ${reason?.message}")
                        onError(Exception("Unable to leave presence: ${reason?.message}"))
                    }
                }
            )
        } catch (ablyException: AblyException) {
            Timber.e(ablyException)
            onError(ablyException)
        }
    }

    override var transportationMode: TransportationMode
        get() = TODO("Not yet implemented")
        set(value) {
            TODO("Not yet implemented")
        }

    override fun refreshResolution() {
        TODO("Not yet implemented")
    }

    override fun stop() {
        stopLocationUpdates()
        ably.close()
    }

    // TODO define threading strategy: https://github.com/ably/ably-asset-tracking-android/issues/22
    @Synchronized
    private fun stopLocationUpdates() {
        if (isTracking) {
            mapboxNavigation.unregisterLocationObserver(locationObserver)
            channelMap.apply {
                values.forEach { leaveChannelPresence(it) }
                clear()
            }
            isTracking = false
            mapboxReplayer?.finish()
            debugConfiguration?.locationHistoryReadyListener?.invoke(mapboxNavigation.retrieveHistory())
            mapboxNavigation.apply {
                toggleHistory(false)
                toggleHistory(true)
            }
        }
    }

    private fun postToMainThread(operation: () -> Unit) {
        Handler(getLooperForMainThread()).post(operation)
    }

    private fun getLooperForMainThread() = Looper.getMainLooper()
}
