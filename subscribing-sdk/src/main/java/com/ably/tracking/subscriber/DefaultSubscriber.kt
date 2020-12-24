package com.ably.tracking.subscriber

import com.ably.tracking.ConnectionConfiguration
import com.ably.tracking.Resolution
import com.ably.tracking.common.ClientTypes
import com.ably.tracking.common.EventNames
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.getGeoJsonMessages
import com.ably.tracking.common.getPresenceData
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
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.launch
import timber.log.Timber

internal class DefaultSubscriber(
    private val connectionConfiguration: ConnectionConfiguration,
    private val rawLocationUpdatedListener: LocationUpdatedListener,
    private val enhancedLocationUpdatedListener: LocationUpdatedListener,
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
        subscribeForRawEvents()
        subscribeForEnhancedEvents()
    }

    private fun subscribeForRawEvents() {
        channel.subscribe(EventNames.RAW) { message ->
            Timber.i("Ably channel message (raw): $message")
            message.getGeoJsonMessages(gson).forEach {
                Timber.d("Received raw location: ${it.synopsis()}")
                enqueue(RawLocationReceivedEvent(it.toLocation()))
            }
        }
    }

    private fun performRawLocationReceived(event: RawLocationReceivedEvent) {
        callback { rawLocationUpdatedListener(event.location) }
    }

    private fun subscribeForEnhancedEvents() {
        channel.subscribe(EventNames.ENHANCED) { message ->
            Timber.i("Ably channel message (enhanced): $message")
            message.getGeoJsonMessages(gson).forEach {
                Timber.d("Received enhanced location: ${it.synopsis()}")
                enqueue(EnhancedLocationReceivedEvent(it.toLocation()))
            }
        }
    }

    private fun performEnhancedLocationReceived(event: EnhancedLocationReceivedEvent) {
        callback { enhancedLocationUpdatedListener(event.location) }
    }

    override fun sendChangeRequest(resolution: Resolution?, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        enqueue(ChangeResolutionEvent(resolution, onSuccess, onError))
    }

    private fun performChangeResolution(event: ChangeResolutionEvent) {
        presenceData = presenceData.copy(resolution = event.resolution)
        channel.presence.update(
            gson.toJson(presenceData),
            object : CompletionListener {
                override fun onSuccess() {
                    enqueue(SuccessEvent(event.onSuccess))
                }

                override fun onError(reason: ErrorInfo?) {
                    enqueue(ErrorEvent(Exception("Unable to change resolution: ${reason?.message}"), event.onError))
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
            channel.presence.subscribe { enqueue(PresenceMessageEvent(it)) }
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

    private fun performPresenceMessage(event: PresenceMessageEvent) {
        when (event.presenceMessage.action) {
            PresenceMessage.Action.present, PresenceMessage.Action.enter -> {
                val data = event.presenceMessage.getPresenceData(gson)
                if (data.type == ClientTypes.PUBLISHER) {
                    notifyAssetIsOnline()
                }
            }
            PresenceMessage.Action.leave -> {
                val data = event.presenceMessage.getPresenceData(gson)
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

    @OptIn(ObsoleteCoroutinesApi::class)
    private fun createEventsChannel(scope: CoroutineScope) =
        scope.actor<SubscriberEvent> {
            for (event in channel) {
                when (event) {
                    is StopSubscriberEvent -> performStopSubscriber()
                    is RawLocationReceivedEvent -> performRawLocationReceived(event)
                    is EnhancedLocationReceivedEvent -> performEnhancedLocationReceived(event)
                    is PresenceMessageEvent -> performPresenceMessage(event)
                    is ErrorEvent -> performEventError(event)
                    is SuccessEvent -> performEventSuccess(event)
                    is ChangeResolutionEvent -> performChangeResolution(event)
                }
            }
        }

    private fun performEventSuccess(event: SuccessEvent) {
        callback { event.onSuccess() }
    }

    private fun performEventError(event: ErrorEvent) {
        callback { event.onError(event.exception) }
    }

    private fun callback(action: () -> Unit) {
        scope.launch(Dispatchers.Main) { action() }
    }

    private fun enqueue(event: SubscriberEvent) {
        scope.launch { eventsChannel.send(event) }
    }
}
