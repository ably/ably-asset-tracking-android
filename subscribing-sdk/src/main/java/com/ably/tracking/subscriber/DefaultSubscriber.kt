package com.ably.tracking.subscriber

import com.ably.tracking.AssetStatusHandler
import com.ably.tracking.ConnectionConfiguration
import com.ably.tracking.ErrorInformation
import com.ably.tracking.FailureResult
import com.ably.tracking.Handler
import com.ably.tracking.LocationHandler
import com.ably.tracking.Resolution
import com.ably.tracking.ResultHandler
import com.ably.tracking.ResultListener
import com.ably.tracking.SuccessResult
import com.ably.tracking.common.ClientTypes
import com.ably.tracking.common.EventNames
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.getGeoJsonMessages
import com.ably.tracking.common.getPresenceData
import com.ably.tracking.common.toJava
import com.ably.tracking.common.toLocation
import com.ably.tracking.toTracking
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
    private val rawLocationHandler: LocationHandler,
    private val enhancedLocationHandler: LocationHandler,
    trackingId: String,
    private val assetStatusHandler: AssetStatusHandler?,
    resolution: Resolution?
) : Subscriber {
    private val ably: AblyRealtime
    private val channel: Channel
    private val gson = Gson()
    private var presenceData = PresenceData(ClientTypes.SUBSCRIBER, resolution)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val eventsChannel: SendChannel<Event>

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
                callback(rawLocationHandler, it.toLocation())
            }
        }
    }

    private fun subscribeForEnhancedEvents() {
        channel.subscribe(EventNames.ENHANCED) { message ->
            Timber.i("Ably channel message (enhanced): $message")
            message.getGeoJsonMessages(gson).forEach {
                Timber.d("Received enhanced location: ${it.synopsis()}")
                callback(enhancedLocationHandler, it.toLocation())
            }
        }
    }

    override fun sendChangeRequest(resolution: Resolution, handler: ResultHandler<Unit>) {
        enqueue(ChangeResolutionEvent(resolution, handler))
    }

    override fun sendChangeRequest(resolution: Resolution, listener: ResultListener<Void?>) {
        sendChangeRequest(resolution, { listener.onResult(it.toJava()) })
    }

    private fun performChangeResolution(event: ChangeResolutionEvent) {
        presenceData = presenceData.copy(resolution = event.resolution)
        channel.presence.update(
            gson.toJson(presenceData),
            object : CompletionListener {
                override fun onSuccess() {
                    callback(event.handler, SuccessResult(Unit))
                }

                override fun onError(reason: ErrorInfo) {
                    callback(event.handler, reason.toTracking())
                }
            }
        )
    }

    override fun stop() {
        enqueue(StopEvent())
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

                    override fun onError(reason: ErrorInfo) {
                        // TODO - handle error
                        // https://github.com/ably/ably-asset-tracking-android/issues/17
                        Timber.e("Unable to enter presence: ${reason.message}")
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

                    override fun onError(reason: ErrorInfo) {
                        // TODO - handle error
                        // https://github.com/ably/ably-asset-tracking-android/issues/17
                        Timber.e("Unable to leave presence: ${reason.message}")
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
        assetStatusHandler?.let { callback(it, true) }
    }

    private fun notifyAssetIsOffline() {
        assetStatusHandler?.let { callback(it, false) }
    }

    @OptIn(ObsoleteCoroutinesApi::class)
    private fun createEventsChannel(scope: CoroutineScope) =
        scope.actor<Event> {
            for (event in channel) {
                when (event) {
                    is StopEvent -> performStopSubscriber()
                    is PresenceMessageEvent -> performPresenceMessage(event)
                    is ChangeResolutionEvent -> performChangeResolution(event)
                }
            }
        }

    /**
     * Send a failure event to the main thread, but only if the scope hasn't been cancelled.
     */
    private fun <T> callback(handler: ResultHandler<T>, errorInformation: ErrorInformation) {
        callback(handler, FailureResult(errorInformation))
    }

    /**
     * Send an event to the main thread, but only if the scope hasn't been cancelled.
     */
    private fun <T> callback(handler: Handler<T>, result: T) {
        scope.launch(Dispatchers.Main) { handler(result) }
    }

    private fun enqueue(event: Event) {
        scope.launch { eventsChannel.send(event) }
    }
}
