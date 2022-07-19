package com.ably.tracking.common

import com.ably.tracking.ConnectionException
import com.ably.tracking.EnhancedLocationUpdate
import com.ably.tracking.ErrorInformation
import com.ably.tracking.LocationUpdate
import com.ably.tracking.common.logging.d
import com.ably.tracking.common.logging.e
import com.ably.tracking.common.logging.i
import com.ably.tracking.common.logging.v
import com.ably.tracking.common.logging.w
import com.ably.tracking.common.message.getEnhancedLocationUpdate
import com.ably.tracking.common.message.getRawLocationUpdate
import com.ably.tracking.common.message.toMessage
import com.ably.tracking.common.message.toMessageJson
import com.ably.tracking.connection.ConnectionConfiguration
import com.ably.tracking.logging.LogHandler
import com.google.gson.Gson
import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.realtime.Channel
import io.ably.lib.realtime.ChannelState
import io.ably.lib.realtime.CompletionListener
import io.ably.lib.realtime.ConnectionState
import io.ably.lib.rest.Auth
import io.ably.lib.types.AblyException
import io.ably.lib.types.ChannelMode
import io.ably.lib.types.ChannelOptions
import io.ably.lib.types.ErrorInfo
import io.ably.lib.types.Message
import io.ably.lib.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
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
     * If a channel for the [trackableId] doesn't exist then it just calls [callback] with success.
     *
     * @param trackableId The ID of the trackable channel.
     * @param listener The function that will be called each time a presence message is received.
     * @param callback The function that will be called when subscribing completes. If something goes wrong it will be called with [ConnectionException].
     */
    fun subscribeForPresenceMessages(
        trackableId: String,
        listener: (PresenceMessage) -> Unit,
        callback: (Result<Unit>) -> Unit,
    )

    /**
     * Sends an enhanced location update to the channel.
     * Should be called only when there's an existing channel for the [trackableId].
     * If a channel for the [trackableId] doesn't exist then it just calls [callback] with success.
     *
     * @param trackableId The ID of the trackable channel.
     * @param locationUpdate The location update that is sent to the channel.
     * @param callback The function that will be called when sending completes. If something goes wrong it will be called with [ConnectionException].
     */
    fun sendEnhancedLocation(
        trackableId: String,
        locationUpdate: EnhancedLocationUpdate,
        callback: (Result<Unit>) -> Unit
    )

    /**
     * Sends a raw location update to the channel.
     * Should be called only when there's an existing channel for the [trackableId].
     * If a channel for the [trackableId] doesn't exist then it just calls [callback] with success.
     *
     * @param trackableId The ID of the trackable channel.
     * @param locationUpdate The location update that is sent to the channel.
     * @param callback The function that will be called when sending completes. If something goes wrong it will be called with [ConnectionException].
     */
    fun sendRawLocation(
        trackableId: String,
        locationUpdate: LocationUpdate,
        callback: (Result<Unit>) -> Unit
    )

    /**
     * Adds a listener for the enhanced location updates that are received from the channel.
     * If a channel for the [trackableId] doesn't exist then nothing happens.
     *
     * @param trackableId The ID of the trackable channel.
     * @param listener The function that will be called each time an enhanced location update event is received.
     *
     * @throws ConnectionException if something goes wrong.
     */
    fun subscribeForEnhancedEvents(trackableId: String, listener: (LocationUpdate) -> Unit)

    /**
     * Adds a listener for the raw location updates that are received from the channel.
     * The raw locations publishing needs to be enabled in the Publisher builder API in order to receive them here.
     * If a channel for the [trackableId] doesn't exist then nothing happens.
     *
     * @param trackableId The ID of the trackable channel.
     * @param listener The function that will be called each time a raw location update event is received.
     *
     * @throws ConnectionException if something goes wrong.
     */
    fun subscribeForRawEvents(trackableId: String, listener: (LocationUpdate) -> Unit)

    /**
     * Joins the presence of the channel for the given [trackableId] and add it to the connected channels.
     * If successfully joined the presence then the channel is added to the connected channels.
     * If a channel for the given [trackableId] exists then it just calls [callback] with success.
     *
     * @param trackableId The ID of the trackable channel.
     * @param presenceData The data that will be send via the presence channel.
     * @param useRewind If set to true then after connecting the channel will replay the last event that was sent in it.
     * @param willPublish If set to true then the connection will allow sending data.
     * @param willSubscribe If set to true then the connection will allow listening for data.
     * @param callback The function that will be called when connecting completes. If something goes wrong it will be called with [ConnectionException].
     */
    fun connect(
        trackableId: String,
        presenceData: PresenceData,
        useRewind: Boolean = false,
        willPublish: Boolean = false,
        willSubscribe: Boolean = false,
        callback: (Result<Unit>) -> Unit
    )

    /**
     * A suspending version of [connect]
     * */
    suspend fun connect(
        trackableId: String,
        presenceData: PresenceData,
        useRewind: Boolean = false,
        willPublish: Boolean = false,
        willSubscribe: Boolean = false
    ): Result<Boolean>

    /**
     * Updates presence data in the [trackableId] channel's presence.
     * Should be called only when there's an existing channel for the [trackableId].
     * If a channel for the [trackableId] doesn't exist then nothing happens.
     *
     * @param trackableId The ID of the trackable channel.
     * @param presenceData The data that will be send via the presence channel.
     * @param callback The function that will be called when updating presence data completes. If something goes wrong it will be called with [ConnectionException].
     */
    fun updatePresenceData(trackableId: String, presenceData: PresenceData, callback: (Result<Unit>) -> Unit)

    /**
     * Removes the [trackableId] channel from the connected channels and leaves the presence of that channel.
     * If a channel for the given [trackableId] doesn't exist then it just calls [callback] with success.
     *
     * @param trackableId The ID of the trackable channel.
     * @param presenceData The data that will be send via the presence channel.
     * @param callback The function that will be called when disconnecting completes. If something goes wrong it will be called with [ConnectionException].
     */
    fun disconnect(trackableId: String, presenceData: PresenceData, callback: (Result<Unit>) -> Unit)

    /**
     * A suspending version of [disconnect]
     * */
    suspend fun disconnect(trackableId: String, presenceData: PresenceData): Result<Unit>

    /**
     * Cleanups and closes all the connected channels and their presence. In the end closes Ably connection.
     *
     * @param presenceData The data that will be send via the presence channels.
     *
     * @throws ConnectionException if something goes wrong.
     */
    suspend fun close(presenceData: PresenceData)
}

private const val CHANNEL_NAME_PREFIX = "tracking:"
private const val AGENT_HEADER_NAME = "ably-asset-tracking-android"
private const val AUTH_TOKEN_CAPABILITY_ERROR_CODE = 40160

class DefaultAbly
/**
 * @throws ConnectionException if something goes wrong during Ably SDK initialization.
 */
constructor(
    connectionConfiguration: ConnectionConfiguration,
    private val logHandler: LogHandler?,
) : Ably {
    private val gson = Gson()
    private val ably: AblyRealtime
    private val channels: MutableMap<String, Channel> = mutableMapOf()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        try {
            val clientOptions = connectionConfiguration.authentication.clientOptions.apply {
                this.agents = mapOf(AGENT_HEADER_NAME to BuildConfig.VERSION_NAME)
                this.idempotentRestPublishing = true
                this.logLevel = Log.VERBOSE
                this.logHandler = Log.LogHandler { severity, tag, msg, tr -> logMessage(severity, tag, msg, tr) }
            }
            ably = AblyRealtime(clientOptions)
        } catch (exception: AblyException) {
            throw exception.errorInfo.toTrackingException()
        }
    }

    private fun logMessage(severity: Int, tag: String?, message: String?, throwable: Throwable?) {
        val messageToLog = "$tag: $message"
        when (severity) {
            Log.VERBOSE -> logHandler?.v(messageToLog, throwable)
            Log.DEBUG -> logHandler?.d(messageToLog, throwable)
            Log.INFO -> logHandler?.i(messageToLog, throwable)
            Log.WARN -> logHandler?.w(messageToLog, throwable)
            Log.ERROR -> logHandler?.e(messageToLog, throwable)
        }
    }

    override fun subscribeForAblyStateChange(listener: (ConnectionStateChange) -> Unit) {
        ably.connection.on { listener(it.toTracking()) }
    }

    override fun subscribeForChannelStateChange(trackableId: String, listener: (ConnectionStateChange) -> Unit) {
        channels[trackableId]?.let { channel ->
            // Emit the current channel state
            channel.state.toTracking().let { currentChannelState ->
                // Initial state is launched in a fire-and-forget manner to not block this method on the listener() call
                scope.launch { listener(ConnectionStateChange(currentChannelState, null)) }
            }
            channel.on { listener(it.toTracking()) }
        }
    }

    /**
     * Enters the presence of a channel. If it can't enter because of the auth token capabilities,
     * a new auth token is requested and the operation is retried once more.
     * @throws ConnectionException if something goes wrong or the retry fails
     */
    private suspend fun enterChannelPresence(channel: Channel, presenceData: PresenceData) {
        try {
            channel.enterPresenceSuspending(presenceData)
        } catch (connectionException: ConnectionException) {
            if (connectionException.errorInformation.code == AUTH_TOKEN_CAPABILITY_ERROR_CODE) {
                val renewAuthResult = renewAuthSuspending()

                renewAuthResult.errorInfo?.let {
                    throw it.toTrackingException()
                }
                Log.d("Ably", Thread.currentThread().name)
                channel.attachSuspending()
                channel.enterPresenceSuspending(presenceData)
            } else {
                throw connectionException
            }
        }
    }

    data class RenewAuthResult(val success: Boolean, val tokenDetails: Auth.TokenDetails?, val errorInfo: ErrorInfo?)

    private suspend fun renewAuthSuspending(): RenewAuthResult {
        return suspendCoroutine { continuation ->
            try {
                ably.auth.renewAuth { success, tokenDetails, errorInfo ->
                    continuation.resume(RenewAuthResult(success, tokenDetails, errorInfo))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                continuation.resumeWithException(e)
            }
        }
    }

    override fun connect(
        trackableId: String,
        presenceData: PresenceData,
        useRewind: Boolean,
        willPublish: Boolean,
        willSubscribe: Boolean,
        callback: (Result<Unit>) -> Unit
    ) {
        if (!channels.contains(trackableId)) {
            val channelName = "$CHANNEL_NAME_PREFIX$trackableId"
            val channelOptions = ChannelOptions().apply {
                val modesList = mutableListOf(ChannelMode.presence, ChannelMode.presence_subscribe)
                if (willPublish) {
                    modesList.add(ChannelMode.publish)
                }
                if (willSubscribe) {
                    modesList.add(ChannelMode.subscribe)
                }
                modes = modesList.toTypedArray()
            }
            try {
                val channel = if (useRewind)
                    ably.channels.get(channelName, channelOptions.apply { params = mapOf("rewind" to "1") })
                else
                    ably.channels.get(channelName, channelOptions)
                scope.launch {
                    try {
                        if (channel.isDetachedOrFailed()) {
                            channel.attachSuspending()
                        }
                        enterChannelPresence(channel, presenceData)
                        channels[trackableId] = channel
                        callback(Result.success(Unit))
                    } catch (connectionException: ConnectionException) {
                        callback(Result.failure(connectionException))
                    }
                }
            } catch (ablyException: AblyException) {
                callback(Result.failure(ablyException.errorInfo.toTrackingException()))
            }
        } else {
            callback(Result.success(Unit))
        }
    }

    override suspend fun connect(
        trackableId: String,
        presenceData: PresenceData,
        useRewind: Boolean,
        willPublish: Boolean,
        willSubscribe: Boolean
    ): Result<Boolean> {
        return suspendCoroutine { continuation ->
            connect(trackableId, presenceData, useRewind, willPublish, willSubscribe) { result ->
                try {
                    result.getOrThrow()
                    continuation.resume(Result.success(true))
                } catch (exception: ConnectionException) {
                    continuation.resume(Result.failure(exception))
                }
            }
        }
    }

    override fun disconnect(trackableId: String, presenceData: PresenceData, callback: (Result<Unit>) -> Unit) {
        scope.launch {
            if (channels.contains(trackableId)) {
                val channelToRemove = channels[trackableId]!!
                try {
                    retryChannelOperationIfConnectionResumeFails(channelToRemove) {
                        disconnectChannel(it, presenceData)
                    }
                    channels.remove(trackableId)
                    callback(Result.success(Unit))
                } catch (exception: ConnectionException) {
                    callback(Result.failure(exception))
                }
            } else {
                callback(Result.success(Unit))
            }
        }
    }

    private suspend fun disconnectChannel(channel: Channel, presenceData: PresenceData) {
        leavePresence(channel, presenceData)
        channel.unsubscribe()
        channel.presence.unsubscribe()
        detachFromChannel(channel)
    }

    private suspend fun leavePresence(channel: Channel, presenceData: PresenceData) {
        suspendCancellableCoroutine<Unit> { continuation ->
            try {
                channel.presence.leave(
                    gson.toJson(presenceData.toMessage()),
                    object : CompletionListener {
                        override fun onSuccess() {
                            continuation.resume(Unit)
                        }

                        override fun onError(reason: ErrorInfo) {
                            continuation.resumeWithException(reason.toTrackingException())
                        }
                    }
                )
            } catch (ablyException: AblyException) {
                continuation.resumeWithException(ablyException.errorInfo.toTrackingException())
            }
        }
    }

    private suspend fun detachFromChannel(channel: Channel) {
        suspendCancellableCoroutine<Unit> { continuation ->
            try {
                channel.detach(object : CompletionListener {
                    override fun onSuccess() {
                        continuation.resume(Unit)
                    }

                    override fun onError(reason: ErrorInfo) {
                        continuation.resumeWithException(reason.toTrackingException())
                    }
                })
            } catch (ablyException: AblyException) {
                continuation.resumeWithException(ablyException.errorInfo.toTrackingException())
            }
        }
    }

    /**
     * A suspend version of the [DefaultAbly.disconnect] method. It waits until disconnection is completed.
     */
    override suspend fun disconnect(trackableId: String, presenceData: PresenceData): Result<Unit> {
        return suspendCancellableCoroutine { continuation ->
            disconnect(trackableId, presenceData) {
                try {
                    it.getOrThrow()
                    continuation.resume(Result.success(Unit))
                } catch (exception: ConnectionException) {
                    continuation.resume(Result.failure(exception))
                }
            }
        }
    }

    override fun sendEnhancedLocation(
        trackableId: String,
        locationUpdate: EnhancedLocationUpdate,
        callback: (Result<Unit>) -> Unit
    ) {
        val trackableChannel = channels[trackableId]
        if (trackableChannel != null) {
            val locationUpdateJson = locationUpdate.toMessageJson(gson)
            logHandler?.d("sendEnhancedLocationMessage: publishing: $locationUpdateJson")
            sendMessage(
                trackableChannel,
                Message(EventNames.ENHANCED, locationUpdateJson).apply {
                    id = "$trackableId${locationUpdate.hashCode()}"
                },
                callback
            )
        } else {
            callback(Result.success(Unit))
        }
    }

    override fun sendRawLocation(
        trackableId: String,
        locationUpdate: LocationUpdate,
        callback: (Result<Unit>) -> Unit
    ) {
        val trackableChannel = channels[trackableId]
        if (trackableChannel != null) {
            val locationUpdateJson = locationUpdate.toMessageJson(gson)
            logHandler?.d("sendRawLocationMessage: publishing: $locationUpdateJson")
            sendMessage(
                trackableChannel,
                Message(EventNames.RAW, locationUpdateJson).apply {
                    id = "$trackableId${locationUpdate.hashCode()}"
                },
                callback
            )
        } else {
            callback(Result.success(Unit))
        }
    }

    private fun sendMessage(channel: Channel, message: Message?, callback: (Result<Unit>) -> Unit) {
        scope.launch {
            try {
                retryChannelOperationIfConnectionResumeFails(channel) {
                    sendMessage(it, message)
                }
                callback(Result.success(Unit))
            } catch (exception: ConnectionException) {
                callback(Result.failure(exception))
            }
        }
    }

    private suspend fun sendMessage(channel: Channel, message: Message?) {
        suspendCancellableCoroutine<Unit> { continuation ->
            try {
                channel.publish(
                    message,
                    object : CompletionListener {
                        override fun onSuccess() {
                            continuation.resume(Unit)
                        }

                        override fun onError(reason: ErrorInfo) {
                            continuation.resumeWithException(reason.toTrackingException())
                        }
                    }
                )
            } catch (exception: AblyException) {
                continuation.resumeWithException(exception.errorInfo.toTrackingException())
            }
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

    override fun subscribeForRawEvents(trackableId: String, listener: (LocationUpdate) -> Unit) {
        channels[trackableId]?.let { channel ->
            try {
                channel.subscribe(EventNames.RAW) { message ->
                    listener(message.getRawLocationUpdate(gson))
                }
            } catch (exception: AblyException) {
                throw exception.errorInfo.toTrackingException()
            }
        }
    }

    override fun subscribeForPresenceMessages(
        trackableId: String,
        listener: (PresenceMessage) -> Unit,
        callback: (Result<Unit>) -> Unit,
    ) {
        val channel = channels[trackableId]
        if (channel != null) {
            // Launching on a separate thread as emitting the current presence messages might block the current thread
            scope.launch {
                try {
                    emitAllCurrentMessagesFromPresence(channel, listener)
                    channel.presence.subscribe {
                        val parsedMessage = it.toTracking(gson)
                        if (parsedMessage != null) {
                            listener(parsedMessage)
                        } else {
                            logHandler?.w("Presence message in unexpected format: $it")
                        }
                    }
                    callback(Result.success(Unit))
                } catch (exception: AblyException) {
                    callback(Result.failure(exception.errorInfo.toTrackingException()))
                }
            }
        } else {
            callback(Result.success(Unit))
        }
    }

    /**
     * Warning: This method might block the current thread due to the presence.get(true) call.
     */
    private fun emitAllCurrentMessagesFromPresence(channel: Channel, listener: (PresenceMessage) -> Unit) {
        channel.presence.get(true).let { messages ->
            messages.forEach { presenceMessage ->
                // Each message is launched in a fire-and-forget manner to not block this method on the listener() call
                scope.launch {
                    val parsedMessage = presenceMessage.toTracking(gson)
                    if (parsedMessage != null) {
                        listener(parsedMessage)
                    } else {
                        logHandler?.w("Presence message in unexpected format: $presenceMessage")
                    }
                }
            }
        }
    }

    override fun updatePresenceData(trackableId: String, presenceData: PresenceData, callback: (Result<Unit>) -> Unit) {
        scope.launch {
            val trackableChannel = channels[trackableId] ?: return@launch
            try {
                retryChannelOperationIfConnectionResumeFails(trackableChannel) {
                    updatePresenceData(it, presenceData)
                }
                callback(Result.success(Unit))
            } catch (exception: ConnectionException) {
                callback(Result.failure(exception))
            }
        }
    }

    private suspend fun updatePresenceData(channel: Channel, presenceData: PresenceData) {
        suspendCancellableCoroutine<Unit> { continuation ->
            try {
                channel.presence.update(
                    gson.toJson(presenceData.toMessage()),
                    object : CompletionListener {
                        override fun onSuccess() {
                            continuation.resume(Unit)
                        }

                        override fun onError(reason: ErrorInfo) {
                            continuation.resumeWithException(reason.toTrackingException())
                        }
                    }
                )
            } catch (exception: AblyException) {
                continuation.resumeWithException(exception.errorInfo.toTrackingException())
            }
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
     * @throws ConnectionException if the [AblyRealtime] state changes to [ConnectionState.failed].
     */
    private suspend fun closeConnection() {
        suspendCancellableCoroutine<Unit> { continuation ->
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

    /**
     * Performs the [operation] and if a "connection resume" exception is thrown it waits for the [channel] to
     * reconnect and retries the [operation], otherwise it rethrows the exception. If the [operation] fails for
     * the second time the exception is rethrown no matter if it was the "connection resume" exception or not.
     */
    private suspend fun retryChannelOperationIfConnectionResumeFails(
        channel: Channel,
        operation: suspend (Channel) -> Unit
    ) {
        try {
            operation(channel)
        } catch (exception: ConnectionException) {
            if (exception.isConnectionResumeException()) {
                logHandler?.w(
                    "Connection resume failed for channel ${channel.name}, waiting for the channel to be reconnected",
                    exception
                )
                try {
                    waitForChannelReconnection(channel)
                    operation(channel)
                } catch (secondException: ConnectionException) {
                    logHandler?.w(
                        "Retrying the operation on channel ${channel.name} has failed for the second time",
                        secondException
                    )
                    throw secondException
                }
            } else {
                throw exception
            }
        }
    }

    /**
     * Waits for the [channel] to change to the [ChannelState.attached] state.
     * If this doesn't happen during the next [timeoutInMilliseconds] milliseconds, then an exception is thrown.
     */
    private suspend fun waitForChannelReconnection(channel: Channel, timeoutInMilliseconds: Long = 10_000L) {
        try {
            withTimeout(timeoutInMilliseconds) {
                suspendCancellableCoroutine<Unit> { continuation ->
                    channel.on { channelStateChange ->
                        if (channelStateChange.current == ChannelState.attached) {
                            continuation.resume(Unit)
                        }
                    }
                }
            }
        } catch (exception: TimeoutCancellationException) {
            throw ConnectionException(ErrorInformation("Timeout was thrown when waiting for channel to attach"))
        }
    }

    private fun ConnectionException.isConnectionResumeException(): Boolean =
        errorInformation.let { it.message == "Connection resume failed" && it.code == 50000 && it.statusCode == 500 }

    /**
     * Enter the presence of the [Channel] and waits for this operation to complete.
     * If something goes wrong then it throws a [ConnectionException].
     */
    private suspend fun Channel.enterPresenceSuspending(presenceData: PresenceData) {
        suspendCancellableCoroutine<Unit> { continuation ->
            try {
                presence.enter(
                    gson.toJson(presenceData.toMessage()),
                    object : CompletionListener {
                        override fun onSuccess() {
                            continuation.resume(Unit)
                        }

                        override fun onError(reason: ErrorInfo) {
                            continuation.resumeWithException(reason.toTrackingException())
                        }
                    }
                )
            } catch (ablyException: AblyException) {
                continuation.resumeWithException(ablyException.errorInfo.toTrackingException())
            }
        }
    }

    /**
     * Attaches the [Channel] and waits for this operation to complete.
     * If something goes wrong then it throws a [ConnectionException].
     */
    private suspend fun Channel.attachSuspending() {
        suspendCancellableCoroutine<Unit> { continuation ->
            try {
                attach(object : CompletionListener {
                    override fun onSuccess() {
                        continuation.resume(Unit)
                    }

                    override fun onError(reason: ErrorInfo) {
                        continuation.resumeWithException(reason.toTrackingException())
                    }
                })
            } catch (ablyException: AblyException) {
                continuation.resumeWithException(ablyException.errorInfo.toTrackingException())
            }
        }
    }

    private fun Channel.isDetachedOrFailed(): Boolean =
        state == ChannelState.detached || state == ChannelState.failed
}
