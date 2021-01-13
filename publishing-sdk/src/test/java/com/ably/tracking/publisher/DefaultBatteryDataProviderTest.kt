package com.ably.tracking.publisher

import android.content.Context
import io.mockk.mockk
import org.junit.Assert
import org.junit.Test

internal class DefaultBatteryDataProviderTest {
    val contextMock = mockk<Context>()
    val batteryDataProvider = DefaultBatteryDataProvider(contextMock)

    @Test
    fun `should calculate battery percentage from level and scale`() {
        // given
        val level = 3
        val scale = 10

        // when
        val batteryPercentage = batteryDataProvider.calculateBatteryPercentage(level, scale)

        // then
        Assert.assertEquals(30F, batteryPercentage, 0.1F)
    }

    @Test
    fun `should limit battery percentage to 100 if calculated value is bigger`() {
        // given
        val level = 600
        val scale = 1

        // when
        val batteryPercentage = batteryDataProvider.calculateBatteryPercentage(level, scale)

        // then
        Assert.assertEquals(100F, batteryPercentage, 0.1F)
    }

    @Test
    fun `should limit battery percentage to 0 if calculated value is smaller`() {
        // given
        val level = -12
        val scale = 1

        // when
        val batteryPercentage = batteryDataProvider.calculateBatteryPercentage(level, scale)

        // then
        Assert.assertEquals(0F, batteryPercentage, 0.1F)
    }
}
