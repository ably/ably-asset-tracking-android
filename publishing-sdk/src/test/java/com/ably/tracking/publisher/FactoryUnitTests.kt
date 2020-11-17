package com.ably.tracking.publisher

import org.junit.Test

/**
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class FactoryUnitTests {
    @Test(expected = NotImplementedError::class)
    fun publishers_factory_to_be_implemented() {
        AssetPublisher.publishers()
    }
}
