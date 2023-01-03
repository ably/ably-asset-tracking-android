package com.ably.tracking.test.common

import com.ably.tracking.ConnectionException
import com.ably.tracking.ErrorInformation
import com.ably.tracking.common.Ably
import com.ably.tracking.common.PresenceData
import com.ably.tracking.common.PresenceMessage
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.every
import io.mockk.slot
import kotlinx.coroutines.delay

fun Ably.mockCreateConnectionSuccess(trackableId: String) {
    mockConnectSuccess(trackableId)
    mockSubscribeToPresenceSuccess(trackableId)
}

fun Ably.mockStartConnectionSuccess() {
    coEvery { startConnection() } returns Result.success(Unit)
}

fun Ably.mockStartConnectionFailure() {
    coEvery { startConnection() } returns Result.failure(anyConnectionException())
}

fun Ably.mockConnectSuccess(trackableId: String) {
    val callbackSlot = slot<(Result<Unit>) -> Unit>()
    every {
        connect(trackableId, any(), any(), any(), any(), capture(callbackSlot))
    } answers {
        callbackSlot.captured(Result.success(Unit))
    }

    coEvery {
        connect(trackableId, any(), any(), any(), any())
    } returns Result.success(Unit)
}

fun Ably.mockConnectFailure(trackableId: String) {
    val callbackSlot = slot<(Result<Unit>) -> Unit>()
    every { connect(trackableId, any(), any(), any(), any(), capture(callbackSlot)) } answers {
        callbackSlot.captured(Result.failure(anyConnectionException()))
    }
    coEvery {
        connect(trackableId, any(), any(), any(), any())
    } returns Result.failure(anyConnectionException())
}

fun Ably.mockConnectFailureThenSuccess(trackableId: String, callbackDelayInMilliseconds: Long? = null) {
    var failed = false
    coEvery {
        connect(trackableId, any(), any(), any(), any())
    }.coAnswers {
        callbackDelayInMilliseconds?.let { delay(it) }
        if (!failed) {
            failed = true
            Result.failure(anyConnectionException())
        } else {
            Result.success(Unit)
        }
    }
}

fun Ably.mockSubscribeToPresenceSuccess(
    trackableId: String,
    listenerSlot: CapturingSlot<(PresenceMessage) -> Unit> = slot()
) {
    coEvery { subscribeForPresenceMessages(trackableId, capture(listenerSlot), any<Boolean>()) } returns Result.success(Unit)
}

fun Ably.mockSubscribeToPresenceError(trackableId: String) {
    coEvery { subscribeForPresenceMessages(trackableId, any(), any<Boolean>()) } returns Result.failure(anyConnectionException())
}

fun Ably.mockGetCurrentPresenceSuccess(
    trackableId: String,
    currentPresenceMessage: List<PresenceMessage> = emptyList(),
) {
    coEvery { getCurrentPresence(trackableId) } returns Result.success(currentPresenceMessage)
}

fun Ably.mockGetCurrentPresenceError(trackableId: String) {
    coEvery { getCurrentPresence(trackableId) } returns Result.failure(anyConnectionException())
}

fun Ably.mockDisconnect(trackableId: String, result: Result<Unit>) {
    val callbackSlot = slot<(Result<Unit>) -> Unit>()
    every {
        disconnect(trackableId, any(), capture(callbackSlot))
    } answers {
        callbackSlot.captured(result)
    }
    coEvery { disconnect(trackableId, any()) } returns result
}

fun Ably.mockDisconnectSuccess(trackableId: String) {
    mockDisconnect(trackableId, Result.success(Unit))
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

fun Ably.mockUpdatePresenceDataSuccess(trackableId: String) {
    val callbackSlot = slot<(Result<Unit>) -> Unit>()
    every {
        updatePresenceData(trackableId, any(), capture(callbackSlot))
    } answers {
        callbackSlot.captured(Result.success(Unit))
    }
    coEvery { updatePresenceData(trackableId, any()) } returns Result.success(Unit)
}

fun Ably.mockSendRawLocationSuccess(trackableId: String) {
    val callbackSlot = slot<(Result<Unit>) -> Unit>()
    every {
        sendRawLocation(trackableId, any(), capture(callbackSlot))
    } answers {
        callbackSlot.captured(Result.success(Unit))
    }
}

fun Ably.mockCloseFailure(exception: ConnectionException = anyConnectionException()) {
    coEvery { close(any()) } throws exception
}

fun Ably.mockCloseSuccessWithDelay(delayInMilliseconds: Long) {
    coEvery { close(any()) } coAnswers { delay(delayInMilliseconds) }
}

private fun anyConnectionException() = ConnectionException(ErrorInformation("Test"))
