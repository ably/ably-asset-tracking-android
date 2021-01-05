package com.ably.tracking

import java.util.concurrent.Semaphore

class TestLock {
    val semaphore = Semaphore(1).apply { acquire() }

    fun block() {
        semaphore.acquire()
    }

    fun resume() {
        semaphore.release()
    }
}
