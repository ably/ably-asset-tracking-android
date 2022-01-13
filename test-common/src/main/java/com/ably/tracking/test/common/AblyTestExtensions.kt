package com.ably.tracking.test.common

import com.ably.tracking.ConnectionException
import com.ably.tracking.ErrorInformation
import com.ably.tracking.common.Ably
import com.ably.tracking.common.PresenceData
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.every
import io.mockk.slot
import kotlinx.coroutines.delay

fun Ably.mockCreateSuspendingConnectionSuccess(trackableId: String) {
    mockSuspendingConnectSuccess(trackableId)
    mockSubscribeToPresenceSuccess(trackableId)
}

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

fun Ably.mockSuspendingConnectSuccess(trackableId: String) {
    coEvery {
        connect(trackableId, any(), any(), any(), any())
    } returns Result.success(true)
}

fun Ably.mockSuspendingConnectFailure(trackableId: String) {
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
            Result.success(true)
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

fun Ably.mockDisconnect(trackableId: String, result: Result<Unit>) {
    val callbackSlot = slot<(Result<Unit>) -> Unit>()
    every {
        disconnect(trackableId, any(), capture(callbackSlot))
    } answers {
        callbackSlot.captured(result)
    }
}

fun Ably.mockDisconnectSuccess(trackableId: String) {
    mockDisconnect(trackableId, Result.success(Unit))
}

fun Ably.mockDisconnectSuccessAndCapturePresenceData(trackableId: String): CapturingSlot<PresenceData> {
    val callbackSlot = slot<(Result<Unit>) -> Unit>()
    val presenceDataSlot = slot<PresenceData>()
    every {
        disconnect(trackableId, capture(presenceDataSlot), capture(callbackSlot))
    } answers {
        callbackSlot.captured(Result.success(Unit))
    }
    return presenceDataSlot
}

fun Ably.mockSuspendingDisconnect(trackableId: String, result: Result<Unit>) {
    coEvery { disconnect(trackableId, any()) } returns result
}

fun Ably.mockSuspendingDisconnectSuccessAndCapturePresenceData(trackableId: String): CapturingSlot<PresenceData> {
    val presenceDataSlot = slot<PresenceData>()
    coEvery { disconnect(trackableId, capture(presenceDataSlot)) } returns Result.success(Unit)
    return presenceDataSlot
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

fun Ably.mockSendRawLocationSuccess(trackableId: String) {
    val callbackSlot = slot<(Result<Unit>) -> Unit>()
    every {
        sendRawLocation(trackableId, any(), capture(callbackSlot))
    } answers {
        callbackSlot.captured(Result.success(Unit))
    }
}

private fun anyConnectionException() = ConnectionException(ErrorInformation("Test"))
