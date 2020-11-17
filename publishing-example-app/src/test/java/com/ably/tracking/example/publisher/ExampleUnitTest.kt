package com.ably.tracking.example.publisher

import com.ably.tracking.publisher.AssetPublisher
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test(expected = NotImplementedError::class)
    fun publishers_factory_to_be_implemented() {
        AssetPublisher.publishers()
    }
}
