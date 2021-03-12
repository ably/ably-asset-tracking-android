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
internal interface Ably {
    /**
     * Adds a listener for the enhanced location updates that are received from the channel.
     *
     * @param listener The function that will be called each time an enhanced location update event is received.
     *
     * @throws com.ably.tracking.AblyException if something goes wrong.
     */
    fun subscribeForEnhancedEvents(listener: (LocationUpdate) -> Unit)

    /**
     * Joins the presence of the channel.
     *
     * @param presenceData The data that will be send via the presence channel.
     * @param callback The function that will be called when connecting completes. If something goes wrong it will be called with [com.ably.tracking.AblyException].
     */
    fun connect(presenceData: PresenceData, callback: (Result<Unit>) -> Unit)

    /**
     * Adds a listener for the presence messages that are received from the channel's presence.
     *
     * @param listener The function that will be called each time a presence message is received.
     *
     * @throws com.ably.tracking.AblyException if something goes wrong.
     */
    fun subscribeForPresenceMessages(listener: (PresenceMessage) -> Unit)

    /**
     * Updates presence data in the channel's presence.
     *
     * @param presenceData The data that will be send via the presence channel.
     * @param callback The function that will be called when updating presence data completes. If something goes wrong it will be called with [com.ably.tracking.AblyException].
     */
    fun updatePresenceData(presenceData: PresenceData, callback: (Result<Unit>) -> Unit)

    /**
     * Cleanups and closes the channel, presence and Ably connections.
     *
     * @param presenceData The data that will be send via the presence channel.
     * @param callback The function that will be called when updating presence data completes. If something goes wrong it will be called with [com.ably.tracking.AblyException].
     */
    fun close(presenceData: PresenceData, callback: (Result<Unit>) -> Unit)
}

internal class DefaultAbly(
    connectionConfiguration: ConnectionConfiguration,
    trackingId: String
) : Ably {
    private val gson = Gson()
    private val ably: AblyRealtime
    private val channel: Channel

    init {
        ably = AblyRealtime(connectionConfiguration.clientOptions)
        channel = ably.channels.get(
            trackingId,
            ChannelOptions().apply { params = mapOf("rewind" to "1") }
        )
    }

    override fun subscribeForEnhancedEvents(listener: (LocationUpdate) -> Unit) {
        try {
            channel.subscribe(EventNames.ENHANCED) { message ->
                Timber.i("Ably channel message (enhanced): $message")
                listener(message.getEnhancedLocationUpdate(gson))
            }
        } catch (exception: AblyException) {
            throw exception.errorInfo.toTrackingException()
        }
    }

    override fun connect(presenceData: PresenceData, callback: (Result<Unit>) -> Unit) {
        try {
            channel.presence.enter(
                gson.toJson(presenceData),
                object : CompletionListener {
                    override fun onSuccess() {
                        callback(Result.success(Unit))
                    }

                    override fun onError(reason: ErrorInfo) {
                        callback(Result.failure(Exception(reason.toTrackingException())))
                    }
                }
            )
        } catch (ablyException: AblyException) {
            callback(Result.failure(Exception(ablyException.errorInfo.toTrackingException())))
        }
    }

    override fun subscribeForPresenceMessages(listener: (PresenceMessage) -> Unit) {
        try {
            channel.presence.subscribe { listener(it.toTracking(gson)) }
        } catch (exception: AblyException) {
            throw exception.errorInfo.toTrackingException()
        }
    }

    override fun updatePresenceData(presenceData: PresenceData, callback: (Result<Unit>) -> Unit) {
        try {
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
        } catch (exception: AblyException) {
            callback(Result.failure(exception.errorInfo.toTrackingException()))
        }
    }

    override fun close(presenceData: PresenceData, callback: (Result<Unit>) -> Unit) {
        channel.unsubscribe()
        leaveChannelPresence(presenceData) {
            if (it.isSuccess) {
                ably.close()
                callback(Result.success(Unit))
            } else {
                callback(it)
            }
        }
    }

    private fun leaveChannelPresence(presenceData: PresenceData, callback: (Result<Unit>) -> Unit) {
        try {
            channel.presence.unsubscribe()
            channel.presence.leave(
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
        } catch (ablyException: AblyException) {
            callback(Result.failure(ablyException.errorInfo.toTrackingException()))
        }
    }
}
