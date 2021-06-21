package com.ably.tracking.test.common

import com.ably.tracking.ConnectionException
import com.ably.tracking.ErrorInformation
import com.ably.tracking.common.Ably
import io.mockk.every
import io.mockk.slot

fun Ably.mockConnectSuccess(trackableId: String) {
    val callbackSlot = slot<(Result<Unit>) -> Unit>()
    every {
        connect(trackableId, any(), any(), capture(callbackSlot))
    } answers {
        callbackSlot.captured(Result.success(Unit))
    }
}

fun Ably.mockSubscribeToPresenceError(trackableId: String) {
    every { subscribeForPresenceMessages(trackableId, any()) } throws ConnectionException(ErrorInformation(""))
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
