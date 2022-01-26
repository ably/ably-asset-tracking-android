package com.ably.tracking.common

/**
 * Provides time of the device. It makes possible to mock time in tests.
 */
interface TimeProvider {
    fun getCurrentTimeInMilliseconds(): Long
}
