package com.ably.tracking.publisher.workerqueue

import com.ably.tracking.publisher.workerqueue.results.AsyncWork
import com.ably.tracking.publisher.workerqueue.results.WorkResult
import org.junit.Assert

internal suspend fun AsyncWork?.assertNotNullAndExecute(): WorkResult {
    Assert.assertNotNull(this)
    return this!!.invoke()
}
