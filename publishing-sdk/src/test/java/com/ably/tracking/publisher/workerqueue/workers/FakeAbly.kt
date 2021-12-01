package com.ably.tracking.publisher.workerqueue.workers

import com.ably.tracking.EnhancedLocationUpdate
import com.ably.tracking.LocationUpdate
import com.ably.tracking.Resolution
import com.ably.tracking.common.Ably
import com.ably.tracking.common.ConnectionStateChange
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.PresenceMessage
import kotlinx.coroutines.delay
import java.lang.Exception

internal class FakeAbly(var connectionSucces: Boolean) : Ably {
    override fun subscribeForAblyStateChange(listener: (ConnectionStateChange) -> Unit) {
        // not needed for now
    }

    override fun subscribeForChannelStateChange(trackableId: String, listener: (ConnectionStateChange) -> Unit) {
        // not needed for now
    }

    override fun subscribeForPresenceMessages(
        trackableId: String,
        listener: (PresenceMessage) -> Unit,
        callback: (Result<Unit>) -> Unit
    ) {
        // not needed for now
    }

    override fun sendEnhancedLocation(
        trackableId: String,
        locationUpdate: EnhancedLocationUpdate,
        callback: (Result<Unit>) -> Unit
    ) {
        // not needed for now
    }

    override fun sendRawLocation(
        trackableId: String,
        locationUpdate: LocationUpdate,
        callback: (Result<Unit>) -> Unit
    ) {
        // not needed for now
    }

    override fun sendResolution(trackableId: String, resolution: Resolution, callback: (Result<Unit>) -> Unit) {
        TODO("Not yet implemented")
    }

    override fun subscribeForEnhancedEvents(trackableId: String, listener: (LocationUpdate) -> Unit) {
        TODO("Not yet implemented")
    }

    override fun subscribeForRawEvents(trackableId: String, listener: (LocationUpdate) -> Unit) {
        TODO("Not yet implemented")
    }

    override fun subscribeForResolutionEvents(trackableId: String, listener: (Resolution) -> Unit) {
        TODO("Not yet implemented")
    }

    override fun connect(
        trackableId: String,
        presenceData: PresenceData,
        useRewind: Boolean,
        willPublish: Boolean,
        willSubscribe: Boolean,
        callback: (Result<Unit>) -> Unit
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun connect(
        trackableId: String,
        presenceData: PresenceData,
        useRewind: Boolean,
        willPublish: Boolean,
        willSubscribe: Boolean
    ): Result<Boolean> {
        delay(1000)
        return if (connectionSucces) Result.success(true) else Result.failure(Exception("connection failed"))
    }

    override fun updatePresenceData(trackableId: String, presenceData: PresenceData, callback: (Result<Unit>) -> Unit) {
        TODO("Not yet implemented")
    }

    override fun disconnect(trackableId: String, presenceData: PresenceData, callback: (Result<Unit>) -> Unit) {
        TODO("Not yet implemented")
    }

    override suspend fun close(presenceData: PresenceData) {
        TODO("Not yet implemented")
    }
}
