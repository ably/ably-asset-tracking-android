package com.ably.tracking.test.common

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
