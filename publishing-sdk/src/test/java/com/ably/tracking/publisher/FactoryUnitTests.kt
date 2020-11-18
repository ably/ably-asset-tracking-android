package com.ably.tracking.publisher

import android.content.Context
import io.mockk.mockk
import org.junit.Assert
import org.junit.Test

/**
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class FactoryUnitTests {
    @Test
    fun `publishers should return AssetPublisher Builder object`() {
        val builder = AssetPublisher.publishers()
        Assert.assertTrue(builder is AssetPublisher.Builder)
    }

    @Test(expected = NotImplementedError::class)
    fun `setting Ably config should throw NotImplementedError`() {
        AssetPublisher.publishers().ablyConfig(AblyConfiguration(""))
    }

    @Test(expected = NotImplementedError::class)
    fun `setting map config should throw NotImplementedError`() {
        AssetPublisher.publishers().mapConfig(MapConfiguration(""))
    }

    @Test(expected = NotImplementedError::class)
    fun `setting logging config should throw NotImplementedError`() {
        AssetPublisher.publishers().logConfig(LogConfiguration(true))
    }

    @Test(expected = NotImplementedError::class)
    fun `setting battery config should throw NotImplementedError`() {
        AssetPublisher.publishers().batteryConfig(BatteryConfiguration(""))
    }

    @Test(expected = NotImplementedError::class)
    fun `setting asset metadata JSON should throw NotImplementedError`() {
        AssetPublisher.publishers().assetMetadataJson("")
    }

    @Test(expected = NotImplementedError::class)
    fun `setting trip metadata JSON should throw NotImplementedError`() {
        AssetPublisher.publishers().tripMetadataJson("")
    }

    @Test(expected = NotImplementedError::class)
    fun `setting location updated listener should throw NotImplementedError`() {
        AssetPublisher.publishers().locationUpdatedListener { }
    }

    @Test(expected = NotImplementedError::class)
    fun `setting android context should throw NotImplementedError`() {
        val mockedContext = mockk<Context>()
        AssetPublisher.publishers().androidContext(mockedContext)
    }

    @Test(expected = NotImplementedError::class)
    fun `setting delivery data should throw NotImplementedError`() {
        AssetPublisher.publishers().delivery("", "", "")
    }

    @Test(expected = NotImplementedError::class)
    fun `calling start should throw NotImplementedError`() {
        AssetPublisher.publishers().start()
    }
}
