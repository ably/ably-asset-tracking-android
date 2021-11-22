package com.ably.tracking.publisher.eventqueue

interface WorkResult{
  fun delay():Long
}

class AddTrackableResult(val delay: Long): WorkResult {
    override fun delay() = delay

}
