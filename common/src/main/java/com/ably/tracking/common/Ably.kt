package com.ably.tracking.common

import com.ably.tracking.ConnectionException
import com.ably.tracking.EnhancedLocationUpdate
import com.ably.tracking.ErrorInformation
import com.ably.tracking.LocationUpdate
import com.ably.tracking.common.logging.createLoggingTag
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
import io.ably.lib.realtime.ChannelState
import io.ably.lib.realtime.CompletionListener
import io.ably.lib.realtime.ConnectionState
import io.ably.lib.realtime.ConnectionStateListener
import io.ably.lib.rest.Auth
import io.ably.lib.types.AblyException
import io.ably.lib.types.ChannelMode
import io.ably.lib.types.ChannelOptions
import io.ably.lib.types.ErrorInfo
import io.ably.lib.types.Message
import io.ably.lib.types.Param
import io.ably.lib.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
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
     * Given a trackable id, wait for the channel to enter the attached state before returning
     * control to the caller.
     */
    suspend fun waitForChannelToAttach(trackableId: String): Result<Unit>

    /**
     * Given a trackable id, get the channel state or null if channel does not exist.
     */
    fun getChannelState(trackableId: String): ChannelState?

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
     * A suspending version of [subscribeForPresenceMessages]
     *
     * @param emitCurrentMessages If set to true it emits messages for each client that's currently in the presence.
     * */
    suspend fun subscribeForPresenceMessages(
        trackableId: String,
        listener: (PresenceMessage) -> Unit,
        emitCurrentMessages: Boolean = true,
    ): Result<Unit>

    /**
     * Returns current messages from the trackable channel's presence.
     * Should be called only when there's an existing channel for the [trackableId].
     * If a channel for the [trackableId] doesn't exist then this method returns a Result with an empty list.
     *
     * @param trackableId The ID of the trackable channel.
     *
     * @return Result containing the current presence messages from the channel's presence. If something goes wrong it will contain a [ConnectionException].
     * */
    suspend fun getCurrentPresence(trackableId: String): Result<List<PresenceMessage>>

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
    fun subscribeForEnhancedEvents(trackableId: String, presenceData: PresenceData, listener: (LocationUpdate) -> Unit)

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
    fun subscribeForRawEvents(trackableId: String, presenceData: PresenceData, listener: (LocationUpdate) -> Unit)

    /**
     * Creates a channel for the given [trackableId], attempts to attach it, and adds it to the connected channels.
     * The channel is added to the connected channels unless attach fails with a fatal exception.
     * If a channel for the given [trackableId] exists then it just calls [callback] with success.
     *
     * @param trackableId The ID of the trackable channel.
     * @param useRewind If set to true then after connecting the channel will replay the last event that was sent in it.
     * @param willPublish If set to true then the connection will allow sending data.
     * @param willSubscribe If set to true then the connection will allow listening for data.
     * @param callback The function that will be called when connecting completes. If something goes wrong it will be called with [ConnectionException].
     */
    fun connect(
        trackableId: String,
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
        useRewind: Boolean = false,
        willPublish: Boolean = false,
        willSubscribe: Boolean = false
    ): Result<Unit>

    /**
     * Joins the presence of the channel for the given [trackableId].
     * If a channel for the [trackableId] doesn't exist then does nothing and returns success.
     *
     * @param trackableId The ID of the trackable channel.
     * @param presenceData The data that will be send via the presence channel.
     */
    suspend fun enterChannelPresence(
        trackableId: String,
        presenceData: PresenceData
    ): Result<Unit>

    /**
     * Updates presence data in the [trackableId] channel's presence.
     * Should be called only when there's an existing channel for the [trackableId].
     * If a channel for the [trackableId] doesn't exist then nothing happens.
     *
     * @param trackableId The ID of the trackable channel.
     * @param presenceData The data that will be send via the presence channel.
     */
    suspend fun updatePresenceData(trackableId: String, presenceData: PresenceData): Result<Unit>

    /**
     * Updates presence data in the [trackableId] channel's presence.
     * Should be called only when there's an existing channel for the [trackableId].
     * Will attempt retry with a 30 seconds timeout.
     * If a channel for the [trackableId] doesn't exist then nothing happens.
     *
     * @param trackableId The ID of the trackable channel.
     * @param presenceData The data that will be send via the presence channel.
     */
    // This function should be removed together with deprecated suspending resolutionPreference function in Subscriber
    suspend fun updatePresenceDataWithRetry(trackableId: String, presenceData: PresenceData): Result<Unit>

    /**
     * Removes the [trackableId] channel from the connected channels and leaves the presence of that channel.
     * If a channel for the given [trackableId] doesn't exist then it just completes.
     *
     * @param trackableId The ID of the trackable channel.
     * @param presenceData The data that will be send via the presence channel.
     */
    suspend fun disconnect(trackableId: String, presenceData: PresenceData)

    /**
     * Cleanups and closes all the connected channels and their presence. In the end closes Ably connection.
     *
     * @param presenceData The data that will be send via the presence channels.
     */
    suspend fun close(presenceData: PresenceData)

    /**
     * Starts the Ably connection. Must be called before any other method.
     */
    suspend fun startConnection(): Result<Unit>

    /**
     * Stops the Ably connection.
     */
    suspend fun stopConnection(): Result<Unit>
}

private const val CHANNEL_NAME_PREFIX = "tracking:"
private const val AGENT_HEADER_NAME = "ably-asset-tracking-android"
private const val AUTH_TOKEN_CAPABILITY_ERROR_CODE = 40160
private const val PRESENCE_LEAVE_MAXIMUM_DURATION_IN_MILLISECONDS = 30_000L
private const val PRESENCE_LEAVE_RETRY_DELAY_IN_MILLISECONDS = 15_000L
private const val STOP_CONNECTION_MAXIMUM_DURATION_IN_MILLISECONDS = 30_000L

class DefaultAbly<ChannelStateListenerType : AblySdkChannelStateListener>
/**
 * @throws ConnectionException If connection configuration is invalid.
 */
constructor(
    private val ablySdkFactory: AblySdkFactory<ChannelStateListenerType>,
    connectionConfiguration: ConnectionConfiguration,
    private val logHandler: LogHandler?,
    private val scope: CoroutineScope,
) : Ably {
    private val gson = Gson()
    private val ably: AblySdkRealtime<ChannelStateListenerType>
    private val TAG = createLoggingTag(this)

    init {
        try {
            val clientOptions = connectionConfiguration.authentication.clientOptions.apply {
                this.agents = mapOf(AGENT_HEADER_NAME to BuildConfig.VERSION_NAME)
                this.idempotentRestPublishing = true
                this.logLevel = Log.VERBOSE
                this.logHandler = Log.LogHandler { severity, tag, msg, tr -> logMessage(severity, tag, msg, tr) }
                this.environment = connectionConfiguration.environment
                this.autoConnect = false
                if (connectionConfiguration.remainPresentForMilliseconds != null) {
                    this.transportParams = arrayOf(
                        Param(
                            "remainPresentFor",
                            connectionConfiguration.remainPresentForMilliseconds.toString()
                        )
                    )
                }
            }
            ably = ablySdkFactory.createRealtime(clientOptions)
        } catch (exception: AblyException) {
            throw exception.errorInfo.toTrackingException().also {
                logHandler?.w("$TAG Failed to create an Ably instance", it)
            }
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
        getChannelIfExists(trackableId)?.let { channel ->
            // Emit the current channel state
            channel.state.toTracking().let { currentChannelState ->
                // Initial state is launched in a fire-and-forget manner to not block this method on the listener() call
                scope.launch { listener(ConnectionStateChange(currentChannelState, null)) }
            }

            val wrappedChannelStateListener = ablySdkFactory.wrapChannelStateListener { _, stateChange ->
                listener(stateChange.toTracking())
            }
            channel.on(wrappedChannelStateListener)
        }
    }

    override suspend fun enterChannelPresence(
        trackableId: String,
        presenceData: PresenceData
    ): Result<Unit> {
        val channel = getChannelIfExists(trackableId) ?: return Result.success(Unit)

        return try {
            enterPresenceSuspending(channel, presenceData)
            Result.success(Unit)
        } catch (connectionException: ConnectionException) {
            logHandler?.w("$TAG Failed to connect for trackable $trackableId", connectionException)
            Result.failure(connectionException)
        }
    }

    override fun connect(
        trackableId: String,
        useRewind: Boolean,
        willPublish: Boolean,
        willSubscribe: Boolean,
        callback: (Result<Unit>) -> Unit
    ) {
        if (getChannelIfExists(trackableId) == null) {
            val channelName = trackableId.toChannelName()
            val channelOptions = createChannelOptions(willPublish, willSubscribe, useRewind)
            try {
                val channel = ably.channels.get(channelName, channelOptions)
                scope.launch {
                    try {
                        connect(channel)
                        callback(Result.success(Unit))
                    } catch (connectionException: ConnectionException) {
                        logHandler?.w(
                            "$TAG Failed to connect for channel ${channel.name}",
                            connectionException
                        )
                        if (connectionException.isFatal()) {
                            ably.channels.release(channelName)
                        }
                        callback(Result.failure(connectionException))
                    }
                }
            } catch (ablyException: AblyException) {
                val trackingException = ablyException.errorInfo.toTrackingException()
                logHandler?.w(
                    "$TAG Failed to connect for trackable $trackableId",
                    trackingException
                )
                callback(Result.failure(trackingException))
            }
        } else {
            callback(Result.success(Unit))
        }
    }

    /**
     * Attaches a channel. If it can't because of the auth token capabilities,
     * a new auth token is requested and the operation is retried once more.
     * @throws ConnectionException if something goes wrong or the retry fails
     */
    private suspend fun connect(channel: AblySdkRealtime.Channel<ChannelStateListenerType>) {
        try {
            attachSuspending(channel)
        } catch (connectionException: ConnectionException) {
            if (connectionException.errorInformation.code == AUTH_TOKEN_CAPABILITY_ERROR_CODE) {
                val renewAuthResult = renewAuthSuspending()

                renewAuthResult.errorInfo?.let {
                    logHandler?.w(
                        "$TAG Failed to renew auth while entering the presence of channel ${channel.name}",
                        it.toTrackingException()
                    )
                    throw it.toTrackingException()
                }
                attachSuspending(channel)
            } else {
                logHandler?.w(
                    "$TAG Failed to enter the presence of channel ${channel.name}",
                    connectionException
                )
                throw connectionException
            }
        }
    }

    data class RenewAuthResult(
        val success: Boolean,
        val tokenDetails: Auth.TokenDetails?,
        val errorInfo: ErrorInfo?
    )

    private suspend fun renewAuthSuspending(): RenewAuthResult {
        return suspendCoroutine { continuation ->
            try {
                ably.auth.renewAuth { success, tokenDetails, errorInfo ->
                    continuation.resume(RenewAuthResult(success, tokenDetails, errorInfo))
                }
            } catch (e: Exception) {
                logHandler?.w("$TAG Failed to renew Ably auth", e)
                e.printStackTrace()
                continuation.resumeWithException(e)
            }
        }
    }

    private fun createChannelOptions(willPublish: Boolean, willSubscribe: Boolean, useRewind: Boolean) =
        ChannelOptions().apply {
            val modesList = mutableListOf(ChannelMode.presence, ChannelMode.presence_subscribe)
            if (willPublish) {
                modesList.add(ChannelMode.publish)
            }
            if (willSubscribe) {
                modesList.add(ChannelMode.subscribe)
            }
            modes = modesList.toTypedArray()

            if (useRewind) {
                params = mapOf("rewind" to "1")
            }
        }

    override suspend fun connect(
        trackableId: String,
        useRewind: Boolean,
        willPublish: Boolean,
        willSubscribe: Boolean
    ): Result<Unit> {
        return suspendCoroutine { continuation ->
            connect(trackableId, useRewind, willPublish, willSubscribe) { result ->
                try {
                    result.getOrThrow()
                    continuation.resume(Result.success(Unit))
                } catch (exception: ConnectionException) {
                    logHandler?.w("$TAG Failed to connect for trackable $trackableId", exception)
                    continuation.resume(Result.failure(exception))
                }
            }
        }
    }

    private suspend fun tryDisconnectChannel(channelToRemove: AblySdkRealtime.Channel<ChannelStateListenerType>, presenceData: PresenceData) =
        try {
            disconnectChannel(channelToRemove, presenceData)
        } catch (exception: Exception) {
            // no-op
        }

    private suspend fun failChannel(channel: AblySdkRealtime.Channel<ChannelStateListenerType>, presenceData: PresenceData, errorInfo: ErrorInfo) {
        leavePresence(channel, presenceData)
        channel.unsubscribe()
        channel.presence.unsubscribe()
        channel.setConnectionFailed(errorInfo)
        channel.off()
        ably.channels.release(channel.name)
    }

    private suspend fun leavePresence(channel: AblySdkRealtime.Channel<ChannelStateListenerType>, presenceData: PresenceData) {
        suspendCancellableCoroutine<Unit> { continuation ->
            try {
                channel.presence.leave(
                    gson.toJson(presenceData.toMessage()),
                    object : CompletionListener {
                        override fun onSuccess() {
                            continuation.resume(Unit)
                        }

                        override fun onError(reason: ErrorInfo?) {
                            val trackingException = reason?.toTrackingException()
                                ?: ConnectionException(ErrorInformation("Unknown error when leaving presence ${channel.name}"))
                            logHandler?.w("$TAG Failed to leave presence for channel ${channel.name}", trackingException)
                            continuation.resumeWithException(trackingException)
                        }
                    }
                )
            } catch (ablyException: AblyException) {
                val trackingException = ablyException.errorInfo.toTrackingException()
                logHandler?.w("$TAG Failed to leave presence for channel ${channel.name}", trackingException)
                continuation.resumeWithException(trackingException)
            }
        }
    }

    override suspend fun disconnect(trackableId: String, presenceData: PresenceData) {
        logHandler?.v("$TAG Disconnect started for trackable $trackableId")
        val channelToRemove = getChannelIfExists(trackableId)
        if (channelToRemove != null) {
            disconnectChannel(channelToRemove, presenceData)
        }
        logHandler?.v("$TAG Disconnect finished for trackable $trackableId")
    }

    private suspend fun disconnectChannel(channelToRemove: AblySdkRealtime.Channel<ChannelStateListenerType>, presenceData: PresenceData) {
        try {
            logHandler?.v("$TAG Starting to disconnect channel ${channelToRemove.name}")
            withTimeout(PRESENCE_LEAVE_MAXIMUM_DURATION_IN_MILLISECONDS) {
                leavePresenceRepeating(channelToRemove, presenceData)
            }
        } catch (connectionException: ConnectionException) {
            logHandler?.w("$TAG Leave presence failed but will continue disconnecting channel ${channelToRemove.name}")
        } catch (timeoutException: TimeoutCancellationException) {
            logHandler?.w("$TAG Leave presence timed out but will continue disconnecting channel ${channelToRemove.name}")
        }
        channelToRemove.unsubscribe()
        channelToRemove.presence.unsubscribe()
        ably.channels.release(channelToRemove.name)
    }

    /**
     * Try to leave the presence of the [channel] and if it fails due to a retriable error repeat the operation
     * after a delay. If the operation fails due to a non-retriable error then re-throw the [ConnectionException].
     */
    private suspend fun leavePresenceRepeating(channel: AblySdkRealtime.Channel<ChannelStateListenerType>, presenceData: PresenceData) {
        try {
            leavePresence(channel, presenceData)
        } catch (connectionException: ConnectionException) {
            if (!connectionException.isRetriable()) {
                logHandler?.w("$TAG Failed to leave presence for channel ${channel.name} due to a non-retriable exception", connectionException)
                throw connectionException
            }
            logHandler?.w("$TAG Failed to leave presence for channel ${channel.name} due to a retriable exception, the operation will be retried in $PRESENCE_LEAVE_RETRY_DELAY_IN_MILLISECONDS ms", connectionException)
            delay(PRESENCE_LEAVE_RETRY_DELAY_IN_MILLISECONDS)
            leavePresenceRepeating(channel, presenceData)
        }
    }

    override fun sendEnhancedLocation(
        trackableId: String,
        locationUpdate: EnhancedLocationUpdate,
        callback: (Result<Unit>) -> Unit
    ) {
        val trackableChannel = getChannelIfExists(trackableId)
        if (trackableChannel != null) {
            val locationUpdateJson = locationUpdate.toMessageJson(gson)
            logHandler?.d("$TAG sendEnhancedLocationMessage: publishing: $locationUpdateJson")
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
        val trackableChannel = getChannelIfExists(trackableId)
        if (trackableChannel != null) {
            val locationUpdateJson = locationUpdate.toMessageJson(gson)
            logHandler?.d("$TAG sendRawLocationMessage: publishing: $locationUpdateJson")
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

    private fun sendMessage(channel: AblySdkRealtime.Channel<ChannelStateListenerType>, message: Message?, callback: (Result<Unit>) -> Unit) {
        scope.launch {
            try {
                sendMessage(channel, message)
                callback(Result.success(Unit))
            } catch (exception: ConnectionException) {
                logHandler?.w("$TAG Failed to send message for channel ${channel.name}", exception)
                callback(Result.failure(exception))
            }
        }
    }

    private suspend fun sendMessage(channel: AblySdkRealtime.Channel<ChannelStateListenerType>, message: Message?) {
        suspendCancellableCoroutine<Unit> { continuation ->
            try {
                channel.publish(
                    message,
                    object : CompletionListener {
                        override fun onSuccess() {
                            continuation.resume(Unit)
                        }

                        override fun onError(reason: ErrorInfo?) {
                            val trackingException = reason?.toTrackingException()
                                ?: ConnectionException(ErrorInformation("Unknown error when sending message ${channel.name}"))
                            logHandler?.w("$TAG Failed to suspend send message for channel ${channel.name}", trackingException)
                            continuation.resumeWithException(trackingException)
                        }
                    }
                )
            } catch (exception: AblyException) {
                val trackingException = exception.errorInfo.toTrackingException()
                logHandler?.w("$TAG Failed to suspend send message for channel ${channel.name}", trackingException)
                continuation.resumeWithException(trackingException)
            }
        }
    }

    override fun subscribeForEnhancedEvents(
        trackableId: String,
        presenceData: PresenceData,
        listener: (LocationUpdate) -> Unit
    ) {
        getChannelIfExists(trackableId)?.let { channel ->
            try {
                channel.subscribe(EventNames.ENHANCED) { message ->
                    processReceivedLocationUpdateMessage(
                        channel,
                        presenceData,
                        message,
                        listener,
                        isRawLocation = false
                    )
                }
            } catch (exception: AblyException) {
                throw exception.errorInfo.toTrackingException().also {
                    logHandler?.w("$TAG Failed to subscriber for enhanced events for channel ${channel.name}", it)
                }
            }
        }
    }

    override fun subscribeForRawEvents(
        trackableId: String,
        presenceData: PresenceData,
        listener: (LocationUpdate) -> Unit
    ) {
        getChannelIfExists(trackableId)?.let { channel ->
            try {
                channel.subscribe(EventNames.RAW) { message ->
                    processReceivedLocationUpdateMessage(
                        channel,
                        presenceData,
                        message,
                        listener,
                        isRawLocation = true
                    )
                }
            } catch (exception: AblyException) {
                throw exception.errorInfo.toTrackingException().also {
                    logHandler?.w("$TAG Failed to subscriber for raw events for channel ${channel.name}", it)
                }
            }
        }
    }

    private fun processReceivedLocationUpdateMessage(
        channel: AblySdkRealtime.Channel<ChannelStateListenerType>,
        presenceData: PresenceData,
        message: Message,
        listener: (LocationUpdate) -> Unit,
        isRawLocation: Boolean
    ) {
        val locationUpdate =
            if (isRawLocation) message.getRawLocationUpdate(gson) else message.getEnhancedLocationUpdate(gson)
        if (locationUpdate != null) {
            listener(locationUpdate)
        } else {
            logHandler?.e(createMalformedLocationUpdateLogMessage(isRawLocation))
            scope.launch {
                failChannel(channel, presenceData, createMalformedMessageErrorInfo())
            }
        }
    }

    private fun createMalformedLocationUpdateLogMessage(isRawLocation: Boolean): String {
        val locationType = if (isRawLocation) "raw" else "enhanced"
        return "$TAG Could not deserialize $locationType location update message, channel will be closed"
    }

    override fun subscribeForPresenceMessages(
        trackableId: String,
        listener: (PresenceMessage) -> Unit,
        callback: (Result<Unit>) -> Unit,
    ) {
        scope.launch {
            val result = subscribeForPresenceMessages(trackableId, listener)
            callback(result)
        }
    }

    override suspend fun subscribeForPresenceMessages(
        trackableId: String,
        listener: (PresenceMessage) -> Unit,
        emitCurrentMessages: Boolean,
    ): Result<Unit> {
        val channel = getChannelIfExists(trackableId) ?: return Result.success(Unit)
        return try {
            if (emitCurrentMessages) {
                emitAllCurrentMessagesFromPresence(channel, listener)
            }
            channel.presence.subscribe {
                val parsedMessage = it.toTracking(gson)
                if (parsedMessage != null) {
                    listener(parsedMessage)
                } else {
                    logHandler?.w("$TAG Presence message in unexpected format: $it")
                }
            }
            Result.success(Unit)
        } catch (exception: AblyException) {
            val trackingException = exception.errorInfo.toTrackingException()
            logHandler?.w("$TAG Failed to subscriber for presence messages for trackable $trackableId", trackingException)
            Result.failure(trackingException)
        }
    }

    private fun emitAllCurrentMessagesFromPresence(channel: AblySdkRealtime.Channel<ChannelStateListenerType>, listener: (PresenceMessage) -> Unit) {
        getAllCurrentMessagesFromPresence(channel).forEach { presenceMessage ->
            // Each message is launched in a fire-and-forget manner to not block this method on the listener() call
            scope.launch { listener(presenceMessage) }
        }
    }

    override suspend fun getCurrentPresence(trackableId: String): Result<List<PresenceMessage>> {
        val channel = getChannelIfExists(trackableId) ?: return Result.success(emptyList())
        return suspendCancellableCoroutine { continuation ->
            try {
                val currentPresenceMessages = getAllCurrentMessagesFromPresence(channel)
                continuation.resume(Result.success(currentPresenceMessages))
            } catch (ablyException: AblyException) {
                val trackingException = ablyException.errorInfo.toTrackingException()
                logHandler?.w("$TAG Failed to get current presence messages for trackable $trackableId", trackingException)
                continuation.resume(Result.failure(trackingException))
            }
        }
    }

    /**
     * Warning: This method might block the current thread due to the presence.get(true) call.
     */
    private fun getAllCurrentMessagesFromPresence(channel: AblySdkRealtime.Channel<ChannelStateListenerType>): List<PresenceMessage> =
        channel.presence.get(true).mapNotNull { presenceMessage ->
            presenceMessage.toTracking(gson).also {
                if (it == null) {
                    logHandler?.w("$TAG Presence message in unexpected format: $presenceMessage")
                }
            }
        }

    override suspend fun updatePresenceData(
        trackableId: String,
        presenceData: PresenceData
    ): Result<Unit> {
        val trackableChannel = getChannelIfExists(trackableId) ?: return Result.success(Unit)
        return try {
            updatePresenceData(trackableChannel, presenceData)
            Result.success(Unit)
        } catch (exception: ConnectionException) {
            logHandler?.w("$TAG Failed to update presence data for trackable $trackableId", exception)
            Result.failure(exception)
        }
    }

    override suspend fun updatePresenceDataWithRetry(trackableId: String, presenceData: PresenceData): Result<Unit> {
        val trackableChannel = getChannelIfExists(trackableId) ?: return Result.success(Unit)
        return try {
            withTimeout(30_000L) {
                updatePresenceDataRepeating(trackableChannel, presenceData)
                Result.success(Unit)
            }
        } catch (exception: ConnectionException) {
            logHandler?.w("$TAG Failed to update presence data for trackable $trackableId", exception)
            Result.failure(exception)
        } catch (exception: TimeoutCancellationException) {
            logHandler?.w("$TAG Failed to update presence data due to a timeout for trackable $trackableId", exception)
            Result.failure(ConnectionException(ErrorInformation("Timeout was thrown when updating presence of channel ${trackableChannel.name}")))
        }
    }

    /**
     * Try to update the presence data of the [channel] and if it fails due to a retriable error repeat the operation
     * after a delay. If the operation fails due to a non-retriable error then re-throw the [ConnectionException].
     *
     * Note: this is a temporary solution that should be removed while fixing https://github.com/ably/ably-asset-tracking-android/issues/962
     */
    private suspend fun updatePresenceDataRepeating(channel: AblySdkRealtime.Channel<ChannelStateListenerType>, presenceData: PresenceData) {
        try {
            updatePresenceData(channel, presenceData)
        } catch (connectionException: ConnectionException) {
            if (!connectionException.isRetriable()) {
                logHandler?.w("$TAG Failed to update presence for channel ${channel.name} due to a non-retriable exception", connectionException)
                throw connectionException
            }
            logHandler?.w("$TAG Failed to update presence for channel ${channel.name} due to a retriable exception, the operation will be retried", connectionException)
            delay(15_000L)
            updatePresenceDataRepeating(channel, presenceData)
        }
    }

    private suspend fun updatePresenceData(channel: AblySdkRealtime.Channel<ChannelStateListenerType>, presenceData: PresenceData) {
        suspendCancellableCoroutine { continuation ->
            try {
                channel.presence.update(
                    gson.toJson(presenceData.toMessage()),
                    object : CompletionListener {
                        override fun onSuccess() {
                            continuation.resume(Unit)
                        }

                        override fun onError(reason: ErrorInfo?) {
                            val trackingException = reason?.toTrackingException()
                                ?: ConnectionException(ErrorInformation("Unknown error when updating presence ${channel.name}"))
                            logHandler?.w("$TAG Failed to suspend update presence data for channel ${channel.name}", trackingException)
                            continuation.resumeWithException(trackingException)
                        }
                    }
                )
            } catch (exception: AblyException) {
                val trackingException = exception.errorInfo.toTrackingException()
                logHandler?.w("$TAG Failed to suspend update presence data for channel ${channel.name}", trackingException)
                continuation.resumeWithException(trackingException)
            }
        }
    }

    private fun getChannelIfExists(trackableId: String): AblySdkRealtime.Channel<ChannelStateListenerType>? {
        val channelName = trackableId.toChannelName()
        return if (ably.channels.containsKey(channelName)) {
            ably.channels.get(channelName)
        } else {
            null
        }
    }

    override suspend fun close(presenceData: PresenceData) {
        // launches closing of all channels in parallel but waits for all channels to be closed
        supervisorScope {
            ably.channels.entrySet().forEach {
                launch {
                    tryDisconnectChannel(it.value, presenceData)
                }
            }
        }
        stopConnection()
    }

    /**
     * Closes [ably] and waits until it's either closed or failed.
     * If the connection is already closed it returns immediately.
     * If the connection is already failed it returns immediately as closing a failed connection should be a no-op
     * according to the Ably features spec (https://sdk.ably.com/builds/ably/specification/main/features/#state-conditions-and-operations).
     *
     * @throws ConnectionException if the [AblySdkRealtime] state changes to [ConnectionState.failed].
     */
    private suspend fun closeSuspending() {
        if (ably.connection.state.isClosed() || ably.connection.state.isFailed() || ably.connection.state.isInitialized()) {
            return
        }
        suspendCancellableCoroutine<Unit> { continuation ->
            ably.connection.on(object : ConnectionStateListener {
                override fun onConnectionStateChanged(connectionStateChange: ConnectionStateListener.ConnectionStateChange) {
                    if (connectionStateChange.current.isClosed()) {
                        ably.connection.off(this)
                        continuation.resume(Unit)
                    } else if (connectionStateChange.current.isFailed()) {
                        ably.connection.off(this)
                        continuation.resumeWithException(connectionStateChange.reason.toTrackingException())
                    }
                }
            })
            ably.close()
        }
    }

    /**
     * For a given trackable, listen for channel state until it enters a state of ONLINE (which in
     * Ably channel-terms means ATTACHED).
     *
     * TODO: At the moment, due to how listeners are wrapped in AAT we're not able to do this the elegant way
     * without rewriting a lot of code. Not ignoring subsequent updates breaks continuations. To get around this for
     * now - ignore any future event updates. We should change this in the future. There shouldn't be a risk of overflows
     * as this is at a channel level - and the channels are retired after a trackable is done with.
     */
    override suspend fun waitForChannelToAttach(trackableId: String): Result<Unit> {

        return suspendCancellableCoroutine { continuation ->
            var resumed = false
            subscribeForChannelStateChange(trackableId) {
                if (resumed) {
                    return@subscribeForChannelStateChange
                }

                if (it.state == com.ably.tracking.common.ConnectionState.ONLINE) {
                    resumed = true
                    continuation.resume(Result.success(Unit))
                }
                if (it.state == com.ably.tracking.common.ConnectionState.FAILED) {
                    resumed = true
                    val errorMessage = "Channel failed whilst waiting for attach: ${it.errorInformation?.message}"
                    logHandler?.w(errorMessage)
                    continuation.resume(Result.failure(ConnectionException(ErrorInformation(errorMessage))))
                }
            }
        }
    }

    override fun getChannelState(trackableId: String): ChannelState? =
        getChannelIfExists(trackableId)?.state

    private fun ConnectionState.isConnected(): Boolean = this == ConnectionState.connected

    private fun ConnectionState.isClosed(): Boolean = this == ConnectionState.closed

    private fun ConnectionState.isFailed(): Boolean = this == ConnectionState.failed

    private fun ConnectionState.isInitialized(): Boolean = this == ConnectionState.initialized

    /**
     * Enter the presence of [channel] and waits for this operation to complete.
     * If something goes wrong then it throws a [ConnectionException].
     */
    private suspend fun enterPresenceSuspending(channel: AblySdkRealtime.Channel<ChannelStateListenerType>, presenceData: PresenceData) {
        logHandler?.i("Enter presence suspending ${channel.name}")
        suspendCancellableCoroutine<Unit> { continuation ->
            try {
                channel.presence.enter(
                    gson.toJson(presenceData.toMessage()),
                    object : CompletionListener {
                        override fun onSuccess() {
                            logHandler?.i("Entered presence on ${channel.name}")
                            continuation.resume(Unit)
                        }

                        override fun onError(reason: ErrorInfo?) {
                            val trackingException = reason?.toTrackingException()
                                ?: ConnectionException(ErrorInformation("Unknown error when entering presence $channel.name"))
                            logHandler?.w("$TAG Failed to suspend enter presence for channel $channel.name", trackingException)
                            continuation.resumeWithException(trackingException)
                        }
                    }
                )
            } catch (ablyException: AblyException) {
                val trackingException = ablyException.errorInfo.toTrackingException()
                logHandler?.w("$TAG Failed to suspend enter presence for channel $channel.name", trackingException)
                continuation.resumeWithException(trackingException)
            }
        }
    }

    /**
     * Attaches [channel] and waits for this operation to complete.
     * If something goes wrong then it throws a [ConnectionException].
     */
    private suspend fun attachSuspending(channel: AblySdkRealtime.Channel<ChannelStateListenerType>) {
        suspendCancellableCoroutine<Unit> { continuation ->
            try {
                channel.attach(object : CompletionListener {
                    override fun onSuccess() {
                        continuation.resume(Unit)
                    }

                    override fun onError(reason: ErrorInfo?) {
                        val trackingException = reason?.toTrackingException()
                            ?: ConnectionException(ErrorInformation("Unknown error when attaching channel $channel.name"))
                        logHandler?.w("$TAG Failed to suspend attach to channel $channel.name", trackingException)
                        continuation.resumeWithException(trackingException)
                    }
                })
            } catch (ablyException: AblyException) {
                val trackingException = ablyException.errorInfo.toTrackingException()
                logHandler?.w("$TAG Failed to suspend attach to channel $channel.name", trackingException)
                continuation.resumeWithException(trackingException)
            }
        }
    }

    private fun createMalformedMessageErrorInfo(): ErrorInfo = ErrorInfo("Received a malformed message", 400, 100_001)

    override suspend fun startConnection(): Result<Unit> {
        return try {
            connectSuspending()
            Result.success(Unit)
        } catch (connectionException: ConnectionException) {
            logHandler?.w("$TAG Failed to start Ably connection", connectionException)
            Result.failure(connectionException)
        }
    }

    override suspend fun stopConnection(): Result<Unit> {
        return try {
            withTimeout(STOP_CONNECTION_MAXIMUM_DURATION_IN_MILLISECONDS) {
                closeSuspending()
            }
            Result.success(Unit)
        } catch (connectionException: ConnectionException) {
            logHandler?.w("$TAG Failed to stop Ably connection", connectionException)
            Result.failure(connectionException)
        } catch (exception: TimeoutCancellationException) {
            logHandler?.w("$TAG Stop Ably connection timed out", exception)
            Result.failure(exception)
        }
    }

    /**
     * A suspending version of the [AblySdkRealtime.connect] method. It will begin connecting [ably] and wait until it's connected.
     * If the connection enters the "failed" or "closed" state it will throw a [ConnectionException].
     * If the instance is already connected it will finish immediately.
     * If the connection is already failed it throws a [ConnectionException].
     *
     * @throws ConnectionException if something goes wrong.
     */
    private suspend fun connectSuspending() {
        if (ably.connection.state.isConnected()) {
            return
        } else if (ably.connection.state.isFailed()) {
            // We expect connection.reason to be non-null if the connection is in a failed state
            throw ably.connection.reason!!.toTrackingException()
        }
        suspendCancellableCoroutine { continuation ->
            ably.connection.on(object : ConnectionStateListener {
                override fun onConnectionStateChanged(connectionStateChange: ConnectionStateListener.ConnectionStateChange) {
                    when {
                        connectionStateChange.current.isConnected() -> {
                            ably.connection.off(this)
                            continuation.resume(Unit)
                        }
                        connectionStateChange.current.isFailed() -> {
                            ably.connection.off(this)
                            continuation.resumeWithException(connectionStateChange.reason.toTrackingException())
                        }
                        connectionStateChange.current.isClosed() -> {
                            ably.connection.off(this)
                            val exception =
                                ConnectionException(ErrorInformation("Connection entered closed state whilst waiting for it to connect"))
                            continuation.resumeWithException(exception)
                        }
                    }
                }
            })
            ably.connect()
        }
    }

    private fun String.toChannelName() = "$CHANNEL_NAME_PREFIX$this"
}
