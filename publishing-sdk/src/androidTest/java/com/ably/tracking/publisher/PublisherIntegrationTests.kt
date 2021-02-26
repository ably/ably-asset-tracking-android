package com.ably.tracking.publisher

import android.annotation.SuppressLint
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ably.tracking.Accuracy
import com.ably.tracking.ConnectionConfiguration
import com.ably.tracking.Resolution
import com.ably.tracking.test.common.UnitExpectation
import com.ably.tracking.test.common.UnitResultExpectation
import com.ably.tracking.test.common.testLogD
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

private const val MAPBOX_ACCESS_TOKEN = BuildConfig.MAPBOX_ACCESS_TOKEN
private const val CLIENT_ID = "IntegrationTestsClient"
private const val ABLY_API_KEY = BuildConfig.ABLY_API_KEY

@RunWith(AndroidJUnit4::class)
class PublisherIntegrationTests {
    val gson = Gson()

    @Test
    fun createAndStartPublisherAndWaitUntilDataEnds() {
        testLogD("##########  PublisherIntegrationTests.createAndStartPublisherAndWaitUntilDataEnds  ##########")

        // given
        testLogD("GIVEN")
        val dataEndedExpectation = UnitExpectation("data ended")
        val trackResultExpectation = UnitResultExpectation("track response")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val locationData = getLocationData(context)

        // when
        testLogD("WHEN")
        val publisher = createAndStartPublisher(
            context,
            locationData = locationData,
            onLocationDataEnded = {
                testLogD("data ended")
                dataEndedExpectation.fulfill()
            }
        )
        runBlocking {
            try {
                publisher.track(Trackable("ID"))
                testLogD("track success")
                trackResultExpectation.fulfill(Result.success(Unit))
            } catch (e: Exception) {
                testLogD("track failed")
            }
        }

        // await asynchronous events
        testLogD("AWAIT")
        dataEndedExpectation.await()
        trackResultExpectation.await()

        // cleanup
        testLogD("CLEANUP")
        val stopResultExpectation = UnitResultExpectation("stop response")
        runBlocking {
            try {
                publisher.stop()
                testLogD("stop succes")
                stopResultExpectation.fulfill(Result.success(Unit))
            } catch (e: Exception) {
                testLogD("stop failed")
            }
        }
        stopResultExpectation.await()

        // then
        testLogD("THEN")
        dataEndedExpectation.assertFulfilled()
        trackResultExpectation.assertSuccess()
        stopResultExpectation.assertSuccess()
    }

    @SuppressLint("MissingPermission")
    private fun createAndStartPublisher(
        context: Context,
        resolution: Resolution = Resolution(Accuracy.BALANCED, 1000L, 0.0),
        locationData: LocationHistoryData,
        onLocationDataEnded: () -> Unit
    ) =
        Publisher.publishers()
            .androidContext(context)
            .connection(ConnectionConfiguration(ABLY_API_KEY, CLIENT_ID))
            .map(MapConfiguration(MAPBOX_ACCESS_TOKEN))
            .resolutionPolicy(DefaultResolutionPolicyFactory(resolution, context))
            .profile(RoutingProfile.CYCLING)
            .debug(DebugConfiguration(locationSource = LocationSourceRaw(locationData, onLocationDataEnded)))
            .start()

    private fun getLocationData(context: Context): LocationHistoryData {
        val historyString = context.assets.open("location_history_small.txt").use { String(it.readBytes()) }
        return gson.fromJson(historyString, LocationHistoryData::class.java)
    }
}
