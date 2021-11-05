package com.ably.tracking.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MapboxLocationProviderTests {
    @Test
    fun shouldNotThrowErrorWhenMapboxIsStartedAndStoppedWithoutStartingTrip() {
        // given
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val mapboxLocationProvider = createMapboxLocationProvider(context)

        // when
        mapboxLocationProvider.stopAndClose()

        // then
    }
}
