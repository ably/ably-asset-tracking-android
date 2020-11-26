package com.ably.tracking.example.publisher

import com.ably.tracking.publisher.Publisher
import org.junit.Test

class PublisherTest {
    @Test
    fun `publishers factory should not throw error`() {
        Publisher.publishers()
    }

    @Test
    fun `publishers should allow to set delivery data with optional params`() {
        Publisher.publishers().delivery("some_id")
    }
}
