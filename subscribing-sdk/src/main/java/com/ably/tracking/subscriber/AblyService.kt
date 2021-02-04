package com.ably.tracking.subscriber

import com.ably.tracking.ConnectionConfiguration
import com.ably.tracking.LocationUpdate
import com.ably.tracking.clientOptions
import com.ably.tracking.common.EventNames
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.common.getEnhancedLocationUpdate
import com.ably.tracking.toTracking
import com.ably.tracking.toTrackingException
import com.google.gson.Gson
import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.realtime.Channel
import io.ably.lib.realtime.CompletionListener
import io.ably.lib.types.AblyException
import io.ably.lib.types.ChannelOptions
import io.ably.lib.types.ErrorInfo
import timber.log.Timber

/**
 * Wrapper for the [AblyRealtime] that's used to interact with the Ably SDK.
 * In the variant for the [Subscriber] the service is created with a tracking ID and only one channel.
 */
internal interface AblyService {
    /**
     * Adds a listener for the enhanced location updates that are received from the channel.
     *
     * @param listener The function that will be called each time an enhanced location update event is received.
     */
    fun subscribeForEnhancedEvents(listener: (LocationUpdate) -> Unit)

    /**
     * Joins the presence of the channel.
     *
     * @param presenceData The data that will be send via the presence channel.
     */
    fun connect(presenceData: PresenceData)

    /**
     * Adds a listener for the presence messages that are received from the channel's presence.
     *
     * @param listener The function that will be called each time a presence message is received.
     */
    fun subscribeForPresenceMessages(listener: (PresenceMessage) -> Unit)

    /**
     * Updates presence data in the channel's presence.
     *
     * @param presenceData The data that will be send via the presence channel.
     * @param callback The function that will be called when updating presence data completes.
     */
    fun updatePresenceData(presenceData: PresenceData, callback: (Result<Unit>) -> Unit)

    /**
     * Cleanups and closes the channel, presence and Ably connections.
     *
     * @param presenceData The data that will be send via the presence channel.
     */
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
            // TODO - what we were supposed to do here
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
        channel.presence.subscribe { listener(it.toTracking(gson)) }
    }

    override fun updatePresenceData(presenceData: PresenceData, callback: (Result<Unit>) -> Unit) {
        channel.presence.update(
            gson.toJson(presenceData),
            object : CompletionListener {
                override fun onSuccess() {
                    callback(Result.success(Unit))
                }

                override fun onError(reason: ErrorInfo) {
                    callback(Result.failure(reason.toTrackingException()))
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
