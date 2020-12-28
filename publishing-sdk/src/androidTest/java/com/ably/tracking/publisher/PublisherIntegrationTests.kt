package com.ably.tracking.publisher

import android.annotation.SuppressLint
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ably.tracking.Accuracy
import com.ably.tracking.ConnectionConfiguration
import com.ably.tracking.Resolution
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch

private const val MAPBOX_ACCESS_TOKEN = BuildConfig.MAPBOX_ACCESS_TOKEN
private const val CLIENT_ID = "IntegrationTestsClient"
private const val ABLY_API_KEY = BuildConfig.ABLY_API_KEY

@RunWith(AndroidJUnit4::class)
class PublisherIntegrationTests {

    @Test
    fun createAndStartPublisherAndWaitUntilDataEnds() {
        // given
        val testLock = TestLock()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val locationData = getLocationData(context)

        // when
        val publisher = createAndStartPublisher(
            context,
            locationData = locationData,
            onLocationDataEnded = { testLock.resume() }
        ).apply {
            track(Trackable("ID"), {}, { throw Exception("Error when starting tracking") })
        }
        testLock.block()
        publisher.stop()

        // then
    }

    @SuppressLint("MissingPermission")
    private fun createAndStartPublisher(
        context: Context,
        resolution: Resolution = Resolution(Accuracy.BALANCED, 1000L, 0.0),
        locationData: String,
        onLocationDataEnded: () -> Unit
    ) =
        Publisher.publishers()
            .androidContext(context)
            .connection(ConnectionConfiguration(ABLY_API_KEY, CLIENT_ID))
            .map(MapConfiguration(MAPBOX_ACCESS_TOKEN))
            .locationUpdatedListener { }
            .resolutionPolicy(DefaultResolutionPolicyFactory(resolution, context))
            .mode(TransportationMode("TBC"))
            .debug(DebugConfiguration(locationSource = LocationSourceRaw(locationData, onLocationDataEnded)))
            .start()

    private fun getLocationData(context: Context) =
        context.assets.open("location_history_small.txt").use { String(it.readBytes()) }
}

private class TestLock {
    val latch = CountDownLatch(1)

    fun block() {
        latch.await()
    }

    fun resume() {
        latch.countDown()
    }
}
