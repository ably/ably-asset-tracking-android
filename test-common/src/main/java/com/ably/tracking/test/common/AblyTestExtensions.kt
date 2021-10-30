package com.ably.tracking.test.common

import com.ably.tracking.ConnectionException
import com.ably.tracking.ErrorInformation
import com.ably.tracking.common.Ably
import io.mockk.every
import io.mockk.slot

fun Ably.mockCreateConnectionSuccess(trackableId: String) {
    mockConnectSuccess(trackableId)
    mockSubscribeToPresenceSuccess(trackableId)
}

fun Ably.mockConnectSuccess(trackableId: String) {
    val callbackSlot = slot<(Result<Unit>) -> Unit>()
    every {
        connect(trackableId, any(), any(), any(), any(), capture(callbackSlot))
    } answers {
        callbackSlot.captured(Result.success(Unit))
    }
}

fun Ably.mockConnectFailureThenSuccess(trackableId: String, callbackDelayInMilliseconds: Long? = null) {
    var hasFailed = false
    val callbackSlot = slot<(Result<Unit>) -> Unit>()
    every {
        connect(trackableId, any(), any(), any(), any(), capture(callbackSlot))
    } answers {
        callbackDelayInMilliseconds?.let { Thread.sleep(it) }
        if (hasFailed) {
            callbackSlot.captured(Result.success(Unit))
        } else {
            hasFailed = true
            callbackSlot.captured(Result.failure(anyConnectionException()))
        }
    }
}

fun Ably.mockSubscribeToPresenceSuccess(trackableId: String) {
    val callbackSlot = slot<(Result<Unit>) -> Unit>()
    every {
        subscribeForPresenceMessages(trackableId, any(), capture(callbackSlot))
    } answers {
        callbackSlot.captured(Result.success(Unit))
    }
}

fun Ably.mockSubscribeToPresenceError(trackableId: String) {
    val callbackSlot = slot<(Result<Unit>) -> Unit>()
    every {
        subscribeForPresenceMessages(trackableId, any(), capture(callbackSlot))
    } answers {
        callbackSlot.captured(Result.failure(anyConnectionException()))
    }
}

fun Ably.mockDisconnectSuccess(trackableId: String) {
    val callbackSlot = slot<(Result<Unit>) -> Unit>()
    every {
        disconnect(trackableId, any(), capture(callbackSlot))
    } answers {
        callbackSlot.captured(Result.success(Unit))
    }
}

fun Ably.mockSendEnhancedLocationSuccess(trackableId: String) {
    val callbackSlot = slot<(Result<Unit>) -> Unit>()
    every {
        sendEnhancedLocation(trackableId, any(), capture(callbackSlot))
    } answers {
        callbackSlot.captured(Result.success(Unit))
    }
}

fun Ably.mockSendEnhancedLocationFailure(trackableId: String) {
    val callbackSlot = slot<(Result<Unit>) -> Unit>()
    every {
        sendEnhancedLocation(trackableId, any(), capture(callbackSlot))
    } answers {
        callbackSlot.captured(Result.failure(anyConnectionException()))
    }
}

fun Ably.mockSendEnhancedLocationFailureThenSuccess(trackableId: String) {
    var hasFailed = false
    val callbackSlot = slot<(Result<Unit>) -> Unit>()
    every {
        sendEnhancedLocation(trackableId, any(), capture(callbackSlot))
    } answers {
        if (hasFailed) {
            callbackSlot.captured(Result.success(Unit))
        } else {
            hasFailed = true
            callbackSlot.captured(Result.failure(anyConnectionException()))
        }
    }
}

private fun anyConnectionException() = ConnectionException(ErrorInformation("Test"))
