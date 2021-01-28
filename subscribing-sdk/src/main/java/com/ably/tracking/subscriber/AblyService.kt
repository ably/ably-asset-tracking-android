package com.ably.tracking.subscriber

import com.ably.tracking.ConnectionConfiguration
import com.ably.tracking.FailureResult
import com.ably.tracking.LocationUpdate
import com.ably.tracking.Result
import com.ably.tracking.SuccessResult
import com.ably.tracking.clientOptions
import com.ably.tracking.common.EventNames
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.getEnhancedLocationUpdate
import com.ably.tracking.toTracking
import com.google.gson.Gson
import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.realtime.Channel
import io.ably.lib.realtime.CompletionListener
import io.ably.lib.types.AblyException
import io.ably.lib.types.ChannelOptions
import io.ably.lib.types.ErrorInfo
import io.ably.lib.types.PresenceMessage
import timber.log.Timber

internal interface AblyService {
    fun subscribeForEnhancedEvents(listener: (LocationUpdate) -> Unit)
    fun connect(presenceData: PresenceData)
    // TODO change to a type that's our internal type
    fun subscribeForPresenceMessages(listener: (PresenceMessage) -> Unit)
    fun updatePresenceData(presenceData: PresenceData, callback: (Result<Unit>) -> Unit)
    fun close(presenceData: PresenceData)
}

internal class DefaultAblyService(
    connectionConfiguration: ConnectionConfiguration,
    trackingId: String
) : AblyService {
    private val gson = Gson()
    private val ably: AblyRealtime
    private val channel: Channel

    init {
        val clientOptions = connectionConfiguration.clientOptions.apply {
            autoConnect = false
        }
        ably = AblyRealtime(clientOptions)
        ably.connection.on {

        }
        ably.connect()
        channel = ably.channels.get(
            trackingId,
            ChannelOptions().apply { params = mapOf("rewind" to "1") }
        )
    }

    override fun subscribeForEnhancedEvents(listener: (LocationUpdate) -> Unit) {
        channel.subscribe(EventNames.ENHANCED) { message ->
            Timber.i("Ably channel message (enhanced): $message")
            listener(message.getEnhancedLocationUpdate(gson))
        }
    }

    override fun connect(presenceData: PresenceData) {
        try {
            channel.presence.enter(
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

    override fun subscribeForPresenceMessages(listener: (PresenceMessage) -> Unit) {
        channel.presence.subscribe { listener(it) }
    }

    override fun updatePresenceData(presenceData: PresenceData, callback: (Result<Unit>) -> Unit) {
        channel.presence.update(
            gson.toJson(presenceData),
            object : CompletionListener {
                override fun onSuccess() {
                    callback(SuccessResult(Unit))
                }

                override fun onError(reason: ErrorInfo) {
                    callback(FailureResult(reason.toTracking()))
                }
            }
        )
    }

    override fun close(presenceData: PresenceData) {
        channel.unsubscribe()
        leaveChannelPresence(presenceData)
        ably.close()
    }

    private fun leaveChannelPresence(presenceData: PresenceData) {
        try {
            channel.presence.unsubscribe()
            channel.presence.leave(
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
}
