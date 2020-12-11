package com.ably.tracking.subscriber

import android.location.Location
import android.os.Handler
import android.os.Looper
import com.ably.tracking.ConnectionConfiguration
import com.ably.tracking.Resolution
import com.ably.tracking.common.ClientTypes
import com.ably.tracking.common.EventNames
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.getPresenceData
import com.ably.tracking.common.getGeoJsonMessages
import com.ably.tracking.common.toLocation
import com.google.gson.Gson
import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.realtime.Channel
import io.ably.lib.realtime.CompletionListener
import io.ably.lib.types.AblyException
import io.ably.lib.types.ChannelOptions
import io.ably.lib.types.ErrorInfo
import io.ably.lib.types.PresenceMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.launch
import timber.log.Timber

internal class DefaultSubscriber(
    private val connectionConfiguration: ConnectionConfiguration,
    rawLocationUpdatedListener: LocationUpdatedListener,
    enhancedLocationUpdatedListener: LocationUpdatedListener,
    trackingId: String,
    private val assetStatusListener: StatusListener?,
    resolution: Resolution?
) : Subscriber {
    private val ably: AblyRealtime
    private val channel: Channel
    private val gson = Gson()
    private var presenceData = PresenceData(ClientTypes.SUBSCRIBER, resolution)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val eventsChannel: SendChannel<SubscriberEvent>

    init {
        eventsChannel = createEventsChannel(scope)
        ably = AblyRealtime(connectionConfiguration.apiKey)
        channel = ably.channels.get(
            trackingId,
            ChannelOptions().apply { params = mapOf("rewind" to "1") }
        )

        Timber.w("Started.")

        joinChannelPresence()
        subscribeForRawEvents(rawLocationUpdatedListener)
        subscribeForEnhancedEvents(enhancedLocationUpdatedListener)
    }

    private fun subscribeForRawEvents(rawLocationUpdatedListener: (Location) -> Unit) {
        channel.subscribe(EventNames.RAW) { message ->
            Timber.i("Ably channel message (raw): $message")
            message.getGeoJsonMessages(gson).forEach {
                Timber.d("Received raw location: ${it.synopsis()}")
                postToMainThread { rawLocationUpdatedListener(it.toLocation()) }
            }
        }
    }

    private fun subscribeForEnhancedEvents(enhancedLocationUpdatedListener: (Location) -> Unit) {
        channel.subscribe(EventNames.ENHANCED) { message ->
            Timber.i("Ably channel message (enhanced): $message")
            message.getGeoJsonMessages(gson).forEach {
                Timber.d("Received enhanced location: ${it.synopsis()}")
                postToMainThread { enhancedLocationUpdatedListener(it.toLocation()) }
            }
        }
    }

    override fun sendChangeRequest(resolution: Resolution, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        presenceData = presenceData.copy(resolution = resolution)
        channel.presence.update(
            gson.toJson(presenceData),
            object : CompletionListener {
                override fun onSuccess() {
                    postToMainThread { onSuccess() }
                }

                override fun onError(reason: ErrorInfo?) {
                    postToMainThread { onError(Exception("Unable to change resolution: ${reason?.message}")) }
                }
            }
        )
    }

    override fun stop() {
        enqueue(StopSubscriberEvent())
    }

    private fun performStopSubscriber() {
        channel.unsubscribe()
        leaveChannelPresence()
        ably.close()
        scope.cancel()
    }

    private fun joinChannelPresence() {
        try {
            notifyAssetIsOffline()
            channel.presence.subscribe { onPresenceMessage(it) }
            channel.presence.enterClient(
                connectionConfiguration.clientId,
                gson.toJson(presenceData),
                object : CompletionListener {
                    override fun onSuccess() = Unit

                    override fun onError(reason: ErrorInfo?) {
                        // TODO - handle error
                        // https://github.com/ably/ably-asset-tracking-android/issues/17
                        Timber.e("Unable to enter presence: ${reason?.message}")
                    }
                }
            )
        } catch (ablyException: AblyException) {
            // TODO - handle exception
            // https://github.com/ably/ably-asset-tracking-android/issues/17
            Timber.e(ablyException)
        }
    }

    private fun leaveChannelPresence() {
        try {
            channel.presence.unsubscribe()
            notifyAssetIsOffline()
            channel.presence.leaveClient(
                connectionConfiguration.clientId,
                gson.toJson(presenceData),
                object : CompletionListener {
                    override fun onSuccess() = Unit

                    override fun onError(reason: ErrorInfo?) {
                        // TODO - handle error
                        // https://github.com/ably/ably-asset-tracking-android/issues/17
                        Timber.e("Unable to leave presence: ${reason?.message}")
                    }
                }
            )
        } catch (ablyException: AblyException) {
            // TODO - handle exception
            // https://github.com/ably/ably-asset-tracking-android/issues/17
            Timber.e(ablyException)
        }
    }

    private fun onPresenceMessage(presenceMessage: PresenceMessage) {
        when (presenceMessage.action) {
            PresenceMessage.Action.present, PresenceMessage.Action.enter -> {
                val data = presenceMessage.getPresenceData(gson)
                if (data.type == ClientTypes.PUBLISHER) {
                    notifyAssetIsOnline()
                }
            }
            PresenceMessage.Action.leave -> {
                val data = presenceMessage.getPresenceData(gson)
                if (data.type == ClientTypes.PUBLISHER) {
                    notifyAssetIsOffline()
                }
            }
            else -> Unit
        }
    }

    private fun notifyAssetIsOnline() {
        assetStatusListener?.let { callback { it(true) } }
    }

    private fun notifyAssetIsOffline() {
        assetStatusListener?.let { callback { it(false) } }
    }

    private fun postToMainThread(operation: () -> Unit) {
        Handler(Looper.getMainLooper()).post(operation)
    }

    private fun createEventsChannel(scope: CoroutineScope) =
        scope.actor<SubscriberEvent> {
            for (event in channel) {
                when (event) {
                    is StopSubscriberEvent -> performStopSubscriber()
                }
            }
        }

    private fun callback(action: () -> Unit) {
        scope.launch(Dispatchers.Main) { action() }
    }

    private fun enqueue(event: SubscriberEvent) {
        scope.launch { eventsChannel.send(event) }
    }
}
