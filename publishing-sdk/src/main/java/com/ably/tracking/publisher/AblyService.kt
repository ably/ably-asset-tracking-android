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

internal interface AblyService {
    fun subscribeForAblyStateChange(listener: (ConnectionStateChange) -> Unit)
    fun subscribeForPresenceMessages(trackableId: String, listener: (PresenceMessage) -> Unit)
    fun sendEnhancedLocation(trackableId: String, locationUpdate: EnhancedLocationUpdate)
    fun connect(trackableId: String, presenceData: PresenceData, callback: (Result<Unit>) -> Unit)
    fun updatePresenceData(trackableId: String, presenceData: PresenceData, callback: (Result<Unit>) -> Unit)
    fun disconnect(trackableId: String, presenceData: PresenceData, callback: (Result<Unit>) -> Unit)
    fun close(presenceData: PresenceData)
}

internal class DefaultAblyService(
    connectionConfiguration: ConnectionConfiguration
) : AblyService {
    private val gson = Gson()
    private val ably: AblyRealtime
    private val channels: MutableMap<String, Channel> = mutableMapOf()

    init {
        val clientOptions = connectionConfiguration.clientOptions.apply {
            autoConnect = false
        }
        ably = AblyRealtime(clientOptions)
        ably.connection.on {
            // TODO - what we were supposed to do here
        }
        ably.connect()
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
