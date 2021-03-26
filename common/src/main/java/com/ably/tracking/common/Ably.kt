package com.ably.tracking.common

import com.ably.tracking.ConnectionConfiguration
import com.ably.tracking.ConnectionStateChange
import com.ably.tracking.EnhancedLocationUpdate
import com.ably.tracking.LocationUpdate
import com.ably.tracking.clientOptions
import com.ably.tracking.toTracking
import com.ably.tracking.toTrackingException
import com.google.gson.Gson
import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.realtime.Channel
import io.ably.lib.realtime.CompletionListener
import io.ably.lib.realtime.ConnectionState
import io.ably.lib.types.AblyException
import io.ably.lib.types.ChannelOptions
import io.ably.lib.types.ErrorInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Wrapper for the [AblyRealtime] that's used to interact with the Ably SDK.
 */
interface Ably {
    /**
     * Adds a listener for the [AblyRealtime] state updates.
     *
     * @param listener The function that will be called each time the [AblyRealtime] state changes.
     */
    fun subscribeForAblyStateChange(listener: (ConnectionStateChange) -> Unit)

    /**
     * Adds a listener for the channel state updates.
     * After adding a listener it will emit the current state of the channel.
     * Should be called only when there's an existing channel for the [trackableId].
     * If a channel for the [trackableId] doesn't exist then nothing happens.
     *
     * @param trackableId The ID of the trackable channel.
     * @param listener The function that will be called each time a channel state changes.
     */
    fun subscribeForChannelStateChange(trackableId: String, listener: (ConnectionStateChange) -> Unit)

    /**
     * Adds a listener for the presence messages that are received from the channel's presence.
     * After adding a listener it will emit [PresenceMessage] for each client that's currently in the presence.
     * Should be called only when there's an existing channel for the [trackableId].
     * If a channel for the [trackableId] doesn't exist then nothing happens.
     *
     * @param trackableId The ID of the trackable channel.
     * @param listener The function that will be called each time a presence message is received.
     *
     * @throws com.ably.tracking.AblyException if something goes wrong.
     */
    fun subscribeForPresenceMessages(trackableId: String, listener: (PresenceMessage) -> Unit)

    /**
     * Sends an enhanced location update to the channel.
     * Should be called only when there's an existing channel for the [trackableId].
     * If a channel for the [trackableId] doesn't exist then nothing happens.
     *
     * @param trackableId The ID of the trackable channel.
     * @param locationUpdate The location update that is sent to the channel.
     *
     * @throws com.ably.tracking.AblyException if something goes wrong.
     */
    fun sendEnhancedLocation(trackableId: String, locationUpdate: EnhancedLocationUpdate)

    /**
     * Adds a listener for the enhanced location updates that are received from the channel.
     * If a channel for the [trackableId] doesn't exist then nothing happens.
     *
     * @param trackableId The ID of the trackable channel.
     * @param listener The function that will be called each time an enhanced location update event is received.
     *
     * @throws com.ably.tracking.AblyException if something goes wrong.
     */
    fun subscribeForEnhancedEvents(trackableId: String, listener: (LocationUpdate) -> Unit)

    /**
     * Joins the presence of the channel for the given [trackableId] and add it to the connected channels.
     * If successfully joined the presence then the channel is added to the connected channels.
     * If a channel for the given [trackableId] exists then it just calls [callback] with success.
     *
     * @param trackableId The ID of the trackable channel.
     * @param presenceData The data that will be send via the presence channel.
     * @param useRewind If set to true then after connecting the channel will replay the last event that was sent in it.
     * @param callback The function that will be called when connecting completes. If something goes wrong it will be called with [com.ably.tracking.AblyException].
     */
    fun connect(
        trackableId: String,
        presenceData: PresenceData,
        useRewind: Boolean = false,
        callback: (Result<Unit>) -> Unit
    )

    /**
     * Updates presence data in the [trackableId] channel's presence.
     * Should be called only when there's an existing channel for the [trackableId].
     * If a channel for the [trackableId] doesn't exist then nothing happens.
     *
     * @param trackableId The ID of the trackable channel.
     * @param presenceData The data that will be send via the presence channel.
     * @param callback The function that will be called when updating presence data completes. If something goes wrong it will be called with [com.ably.tracking.AblyException].
     */
    fun updatePresenceData(trackableId: String, presenceData: PresenceData, callback: (Result<Unit>) -> Unit)

    /**
     * Removes the [trackableId] channel from the connected channels and leaves the presence of that channel.
     * If a channel for the given [trackableId] doesn't exist then it just calls [callback] with success.
     *
     * @param trackableId The ID of the trackable channel.
     * @param presenceData The data that will be send via the presence channel.
     * @param callback The function that will be called when disconnecting completes. If something goes wrong it will be called with [com.ably.tracking.AblyException].
     */
    fun disconnect(trackableId: String, presenceData: PresenceData, callback: (Result<Unit>) -> Unit)

    /**
     * Cleanups and closes all the connected channels and their presence. In the end closes Ably connection.
     *
     * @param presenceData The data that will be send via the presence channels.
     *
     * @throws com.ably.tracking.AblyException if something goes wrong.
     */
    suspend fun close(presenceData: PresenceData)
}

class DefaultAbly(
    connectionConfiguration: ConnectionConfiguration
) : Ably {
    private val gson = Gson()
    private val ably: AblyRealtime
    private val channels: MutableMap<String, Channel> = mutableMapOf()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        ably = AblyRealtime(connectionConfiguration.clientOptions)
    }

    override fun subscribeForAblyStateChange(listener: (ConnectionStateChange) -> Unit) {
        ably.connection.on { listener(it.toTracking()) }
    }

    override fun subscribeForChannelStateChange(trackableId: String, listener: (ConnectionStateChange) -> Unit) {
        channels[trackableId]?.let { channel ->
            // Emit the current channel state
            channel.state.toTracking().let { currentChannelState ->
                // Initial state is launched in a fire-and-forget manner to not block this method on the listener() call
                scope.launch { listener(ConnectionStateChange(currentChannelState, currentChannelState, null)) }
            }
            channel.on { listener(it.toTracking()) }
        }
    }

    override fun connect(
        trackableId: String,
        presenceData: PresenceData,
        useRewind: Boolean,
        callback: (Result<Unit>) -> Unit
    ) {
        if (!channels.contains(trackableId)) {
            val channel = if (useRewind)
                ably.channels.get(trackableId, ChannelOptions().apply { params = mapOf("rewind" to "1") })
            else
                ably.channels.get(trackableId)
            channel.apply {
                try {
                    presence.enter(
                        gson.toJson(presenceData.toMessage()),
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
                    callback(Result.failure(ablyException.errorInfo.toTrackingException()))
                }
            }
        } else {
            callback(Result.success(Unit))
        }
    }

    override fun disconnect(trackableId: String, presenceData: PresenceData, callback: (Result<Unit>) -> Unit) {
        val removedChannel = channels.remove(trackableId)
        if (removedChannel != null) {
            removedChannel.unsubscribe()
            removedChannel.presence.unsubscribe()
            try {
                removedChannel.presence.leave(
                    gson.toJson(presenceData.toMessage()),
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
        } else {
            callback(Result.success(Unit))
        }
    }

    /**
     * A suspend version of the [DefaultAbly.disconnect] method. It waits until disconnection is completed.
     * @throws com.ably.tracking.AblyException if something goes wrong during disconnect.
     */
    private suspend fun disconnect(trackableId: String, presenceData: PresenceData) {
        suspendCoroutine<Unit> { continuation ->
            disconnect(trackableId, presenceData) {
                try {
                    continuation.resume(it.getOrThrow())
                } catch (exception: com.ably.tracking.AblyException) {
                    continuation.resumeWithException(exception)
                }
            }
        }
    }

    override fun sendEnhancedLocation(trackableId: String, locationUpdate: EnhancedLocationUpdate) {
        val locationUpdateJson = locationUpdate.toJson(gson)
        Timber.d("sendEnhancedLocationMessage: publishing: $locationUpdateJson")
        try {
            channels[trackableId]?.publish(EventNames.ENHANCED, locationUpdateJson)
        } catch (exception: AblyException) {
            throw exception.errorInfo.toTrackingException()
        }
    }

    override fun subscribeForEnhancedEvents(trackableId: String, listener: (LocationUpdate) -> Unit) {
        channels[trackableId]?.let { channel ->
            try {
                channel.subscribe(EventNames.ENHANCED) { message ->
                    listener(message.getEnhancedLocationUpdate(gson))
                }
            } catch (exception: AblyException) {
                throw exception.errorInfo.toTrackingException()
            }
        }
    }

    override fun subscribeForPresenceMessages(trackableId: String, listener: (PresenceMessage) -> Unit) {
        channels[trackableId]?.let { channel ->
            try {
                // Emit the current presence messages of the channel
                channel.presence.get(true).let { messages ->
                    messages.forEach {
                        // Each message is launched in a fire-and-forget manner to not block this method on the listener() call
                        scope.launch { listener(it.toTracking(gson)) }
                    }
                }
                channel.presence.subscribe { listener(it.toTracking(gson)) }
            } catch (exception: AblyException) {
                throw exception.errorInfo.toTrackingException()
            }
        }
    }

    override fun updatePresenceData(trackableId: String, presenceData: PresenceData, callback: (Result<Unit>) -> Unit) {
        try {
            channels[trackableId]?.presence?.update(
                gson.toJson(presenceData.toMessage()),
                object : CompletionListener {
                    override fun onSuccess() {
                        callback(Result.success(Unit))
                    }

                    override fun onError(reason: ErrorInfo) {
                        callback(Result.failure(Exception(reason.toTrackingException())))
                    }
                }
            )
        } catch (exception: AblyException) {
            callback(Result.failure(exception.errorInfo.toTrackingException()))
        }
    }

    override suspend fun close(presenceData: PresenceData) {
        // launches closing of all channels in parallel but waits for all channels to be closed
        supervisorScope {
            channels.keys.forEach { trackableId ->
                launch { disconnect(trackableId, presenceData) }
            }
        }
        closeConnection()
    }

    /**
     * Closes [AblyRealtime] and waits until it's either closed or failed.
     * @throws com.ably.tracking.AblyException if the [AblyRealtime] state changes to [ConnectionState.failed].
     */
    private suspend fun closeConnection() {
        suspendCoroutine<Unit> { continuation ->
            ably.connection.on {
                if (it.current == ConnectionState.closed) {
                    continuation.resume(Unit)
                } else if (it.current == ConnectionState.failed) {
                    continuation.resumeWithException(it.reason.toTrackingException())
                }
            }
            ably.close()
        }
    }
}
