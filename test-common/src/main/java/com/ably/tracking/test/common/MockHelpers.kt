package com.ably.tracking.test.common

import io.mockk.CapturingSlot
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

/**
 * Helper function that blocks test and waits until the slot's value is captured.
 * If the value is not captured in [timeoutInMilliseconds] then a timeout exception is thrown.
 */
fun <T : Any> CapturingSlot<T>.waitForCapture(timeoutInMilliseconds: Long = 5_000L) {
    runBlocking {
        withTimeout(timeoutInMilliseconds) {
            while (!isCaptured) {
                delay(50L)
            }
        }
    }
}
