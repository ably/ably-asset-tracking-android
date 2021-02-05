package com.ably.tracking.publisher

import com.ably.tracking.ConnectionConfiguration
import com.ably.tracking.ConnectionStateChange
import com.ably.tracking.EnhancedLocationUpdate
import com.ably.tracking.clientOptions
import com.ably.tracking.common.EventNames
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.PresenceMessage
import com.ably.tracking.common.toJson
import com.ably.tracking.toTracking
import com.ably.tracking.toTrackingException
import com.google.gson.Gson
import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.realtime.Channel
import io.ably.lib.realtime.CompletionListener
import io.ably.lib.types.AblyException
import io.ably.lib.types.ErrorInfo
import timber.log.Timber

/**
 * Wrapper for the [AblyRealtime] that's used to interact with the Ably SDK.
 * In the variant for the [Publisher] the service is created without any channels. They are created later when you call [connect].
 */
internal interface Ably {
    /**
     * Adds a listener for the [AblyRealtime] state updates.
     *
     * @param listener The function that will be called each time the [AblyRealtime] state changes.
     */
    fun subscribeForAblyStateChange(listener: (ConnectionStateChange) -> Unit)

    /**
     * Adds a listener for the presence messages that are received from the channel's presence.
     * Should be called only when there's an existing channel for the [trackableId].
     * If a channel for the [trackableId] doesn't exist then nothing happens.
     *
     * @param trackableId The ID of the trackable channel.
     * @param listener The function that will be called each time a presence message is received.
     */
    fun subscribeForPresenceMessages(trackableId: String, listener: (PresenceMessage) -> Unit)

    /**
     * Sends an enhanced location update to the channel.
     * Should be called only when there's an existing channel for the [trackableId].
     * If a channel for the [trackableId] doesn't exist then nothing happens.
     *
     * @param trackableId The ID of the trackable channel.
     * @param locationUpdate The location update that is sent to the channel.
     */
    fun sendEnhancedLocation(trackableId: String, locationUpdate: EnhancedLocationUpdate)

    /**
     * Joins the presence of the channel for the given [trackableId] and add it to the connected channels.
     * If successfully joined the presence then the channel is added to the connected channels.
     * If a channel for the given [trackableId] exists then it just calls [callback] with success.
     *
     * @param trackableId The ID of the trackable channel.
     * @param presenceData The data that will be send via the presence channel.
     * @param callback The function that will be called when connecting completes.
     */
    fun connect(trackableId: String, presenceData: PresenceData, callback: (Result<Unit>) -> Unit)

    /**
     * Updates presence data in the [trackableId] channel's presence.
     * Should be called only when there's an existing channel for the [trackableId].
     * If a channel for the [trackableId] doesn't exist then nothing happens.
     *
     * @param trackableId The ID of the trackable channel.
     * @param presenceData The data that will be send via the presence channel.
     * @param callback The function that will be called when connecting completes.
     */
    fun updatePresenceData(trackableId: String, presenceData: PresenceData, callback: (Result<Unit>) -> Unit)

    /**
     * Removes the [trackableId] channel from the connected channels and leaves the presence of that channel.
     * If a channel for the given [trackableId] doesn't exist then it just calls [callback] with success.
     *
     * @param trackableId The ID of the trackable channel.
     * @param presenceData The data that will be send via the presence channel.
     * @param callback The function that will be called when connecting completes.
     */
    fun disconnect(trackableId: String, presenceData: PresenceData, callback: (Result<Unit>) -> Unit)

    /**
     * Cleanups and closes all the connected channels and their presence. In the end closes Ably connection.
     *
     * @param presenceData The data that will be send via the presence channels.
     */
    fun close(presenceData: PresenceData)
}

internal class DefaultAbly(
    connectionConfiguration: ConnectionConfiguration
) : Ably {
    private val gson = Gson()
    private val ably: AblyRealtime
    private val channels: MutableMap<String, Channel> = mutableMapOf()

    init {
        ably = AblyRealtime(connectionConfiguration.clientOptions)
    }

    override fun subscribeForAblyStateChange(listener: (ConnectionStateChange) -> Unit) {
        ably.connection.on { listener(it.toTracking()) }
    }

    override fun connect(trackableId: String, presenceData: PresenceData, callback: (Result<Unit>) -> Unit) {
        if (!channels.contains(trackableId)) {
            ably.channels.get(trackableId).apply {
                try {
                    presence.enter(
                        gson.toJson(presenceData),
                        object : CompletionListener {
                            override fun onSuccess() {
                                channels[trackableId] = this@apply
                                callback(Result.success(Unit))
                            }

                            override fun onError(reason: ErrorInfo) {
                                callback(Result.failure(Exception(reason.toTrackingException())))
                            }
                        }
                    )
                } catch (ablyException: AblyException) {
                    callback(Result.failure(ablyException))
                }
            }
        } else {
            callback(Result.success(Unit))
        }
    }

    override fun disconnect(trackableId: String, presenceData: PresenceData, callback: (Result<Unit>) -> Unit) {
        val removedChannel = channels.remove(trackableId)
        if (removedChannel != null) {
            removedChannel.presence.unsubscribe()
            try {
                removedChannel.presence.leave(
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
                callback(Result.failure(ablyException))
            }
        } else {
            callback(Result.success(Unit))
        }
    }

    override fun sendEnhancedLocation(trackableId: String, locationUpdate: EnhancedLocationUpdate) {
        val locationUpdateJson = locationUpdate.toJson(gson)
        Timber.d("sendEnhancedLocationMessage: publishing: $locationUpdateJson")
        channels[trackableId]?.publish(EventNames.ENHANCED, locationUpdateJson)
    }

    override fun subscribeForPresenceMessages(trackableId: String, listener: (PresenceMessage) -> Unit) {
        channels[trackableId]?.presence?.subscribe { listener(it.toTracking(gson)) }
    }

    override fun updatePresenceData(trackableId: String, presenceData: PresenceData, callback: (Result<Unit>) -> Unit) {
        channels[trackableId]?.presence?.update(
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
    }

    override fun close(presenceData: PresenceData) {
        channels.apply {
            values.forEach {
                it.presence.unsubscribe()
                // TODO leave channel presence
            }
            channels.clear()
        }
        ably.close()
    }
}
