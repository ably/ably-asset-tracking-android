package com.ably.tracking.example.publisher

import com.ably.tracking.publisher.AssetPublisher
import org.junit.Test

class AssetPublisherTest {
    @Test
    fun `publishers factory should not throw error`() {
        AssetPublisher.publishers()
    }
}
