package com.ably.tracking.subscriber

import android.location.Location
import android.os.Handler
import android.os.Looper
import com.google.gson.Gson
import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.realtime.Channel
import io.ably.lib.realtime.CompletionListener
import io.ably.lib.types.AblyException
import io.ably.lib.types.ChannelOptions
import io.ably.lib.types.ErrorInfo
import timber.log.Timber

internal class DefaultAssetSubscriber(
    private val ablyConfiguration: AblyConfiguration,
    rawLocationUpdatedListener: LocationUpdatedListener,
    enhancedLocationUpdatedListener: LocationUpdatedListener,
    trackingId: String
) : AssetSubscriber {
    private val ably: AblyRealtime
    private val channel: Channel
    private val gson = Gson()

    init {
        ably = AblyRealtime(ablyConfiguration.apiKey)
        channel = ably.channels.get(
            trackingId,
            ChannelOptions().apply { params = mapOf("rewind" to "1") }
        )

        Timber.w("Started.")

        joinChannelPresence()
        subscribeForRawEvents(rawLocationUpdatedListener)
        subscribeForEnhancedEvents(enhancedLocationUpdatedListener)
    }

    private fun subscribeForRawEvents(rawLocationUpdatedListener: (Location) -> Unit) {
        channel.subscribe(EventNames.RAW) { message ->
            Timber.i("Ably channel message (raw): $message")
            message.getGeoJsonMessages(gson).forEach {
                Timber.d("Received raw location: ${it.synopsis()}")
                postToMainThread { rawLocationUpdatedListener(it.toLocation()) }
            }
        }
    }

    private fun subscribeForEnhancedEvents(enhancedLocationUpdatedListener: (Location) -> Unit) {
        channel.subscribe(EventNames.ENHANCED) { message ->
            Timber.i("Ably channel message (enhanced): $message")
            message.getGeoJsonMessages(gson).forEach {
                Timber.d("Received enhanced location: ${it.synopsis()}")
                postToMainThread { enhancedLocationUpdatedListener(it.toLocation()) }
            }
        }
    }

    override fun stop() {
        channel.unsubscribe()
        leaveChannelPresence()
        ably.close()
    }

    private fun joinChannelPresence() {
        try {
            channel.presence.enterClient(
                ablyConfiguration.clientId, "",
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
            channel.presence.leaveClient(
                ablyConfiguration.clientId, "",
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

    private fun postToMainThread(operation: () -> Unit) {
        Handler(Looper.getMainLooper()).post(operation)
    }
}
