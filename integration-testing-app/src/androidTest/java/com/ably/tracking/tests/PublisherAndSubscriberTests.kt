package com.ably.tracking.tests

import android.annotation.SuppressLint
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ably.tracking.Accuracy
import com.ably.tracking.connection.BasicAuthenticationConfiguration
import com.ably.tracking.LocationUpdate
import com.ably.tracking.Resolution
import com.ably.tracking.publisher.DefaultResolutionPolicyFactory
import com.ably.tracking.publisher.LocationHistoryData
import com.ably.tracking.publisher.LocationSourceRaw
import com.ably.tracking.publisher.MapConfiguration
import com.ably.tracking.publisher.Publisher
import com.ably.tracking.publisher.RoutingProfile
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.subscriber.Subscriber
import com.ably.tracking.test.common.BooleanExpectation
import com.ably.tracking.test.common.UnitExpectation
import com.ably.tracking.test.common.testLogD
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

private const val MAPBOX_ACCESS_TOKEN = BuildConfig.MAPBOX_ACCESS_TOKEN
private const val CLIENT_ID = "IntegrationTestsClient"
private const val ABLY_API_KEY = BuildConfig.ABLY_API_KEY

@RunWith(AndroidJUnit4::class)
class PublisherAndSubscriberTests {
    val gson = Gson()

    @Test
    fun createAndStartPublisherAndSubscriberAndWaitUntilDataEnds() {
        // given
        val dataEndedExpectation = UnitExpectation("data ended")
        val publisherStoppedExpectation = UnitExpectation("publisher stopped")
        val subscriberStoppedExpectation = UnitExpectation("subscriber stopped")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val trackableId = UUID.randomUUID().toString()
        val locationData = getLocationData(context)
        val publishedLocations = mutableListOf<LocationUpdate>()
        val receivedLocations = mutableListOf<LocationUpdate>()
        val trackExpectation = BooleanExpectation("track response")
        val scope = CoroutineScope(Dispatchers.Default)

        // when
        var subscriber: Subscriber
        runBlocking {
            subscriber = createAndStartSubscriber(trackableId)
        }

        subscriber.locations
            .onEach { receivedLocations.add(it) }
            .launchIn(scope)

        // TODO replace this sleep with an await on an expectation that the subscriber is ready (connected and subscribed)
        Thread.sleep(4000)

        val publisher = createAndStartPublisher(
            context,
            locationData = locationData,
            onLocationDataEnded = {
                dataEndedExpectation.fulfill()
            }
        )

        publisher.locations
            .onEach { publishedLocations.add(it) }
            .launchIn(scope)

        runBlocking {
            try {
                publisher.track(Trackable(trackableId))
                testLogD("track success")
                trackExpectation.fulfill(true)
            } catch (e: Exception) {
                testLogD("track failed")
                trackExpectation.fulfill(false)
            }
        }

        // await
        dataEndedExpectation.await()

        // TODO replace this sleep with an await on an expectation
        Thread.sleep(6000)

        testLogD("COUNT published ${publishedLocations.size}") // observed 15
        testLogD("COUNT received ${receivedLocations.size}") // observed 12

        // cleanup
        scope.launch {
            publisher.stop()
            publisherStoppedExpectation.fulfill()
        }
        scope.launch {
            subscriber.stop()
            subscriberStoppedExpectation.fulfill()
        }
        publisherStoppedExpectation.await()
        subscriberStoppedExpectation.await()

        // then
        dataEndedExpectation.assertFulfilled()
        trackExpectation.assertSuccess()
        publisherStoppedExpectation.assertFulfilled()
        subscriberStoppedExpectation.assertFulfilled()
        Assert.assertTrue(
            "Subscriber should receive at least half the number of events published (received: ${receivedLocations.size}, published: ${publishedLocations.size})",
            receivedLocations.size >= publishedLocations.size / 2
        )
        receivedLocations.forEachIndexed { index, receivedLocation ->
            Assert.assertEquals(
                "Received Subscriber location should be equal to published Publisher location (index $index)",
                publishedLocations[index],
                receivedLocation
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun createAndStartPublisher(
        context: Context,
        resolution: Resolution = Resolution(Accuracy.BALANCED, 1L, 0.0),
        locationData: LocationHistoryData,
        onLocationDataEnded: () -> Unit = {}
    ) =
        Publisher.publishers()
            .androidContext(context)
            .connection(BasicAuthenticationConfiguration.create(ABLY_API_KEY, CLIENT_ID))
            .map(MapConfiguration(MAPBOX_ACCESS_TOKEN))
            .resolutionPolicy(DefaultResolutionPolicyFactory(resolution, context))
            .profile(RoutingProfile.DRIVING)
            .locationSource(LocationSourceRaw.create(locationData, onLocationDataEnded))
            .start()

    private suspend fun createAndStartSubscriber(
        trackingId: String,
        resolution: Resolution = Resolution(Accuracy.BALANCED, 1L, 0.0)
    ) =
        Subscriber.subscribers()
            .connection(BasicAuthenticationConfiguration.create(ABLY_API_KEY, CLIENT_ID))
            .resolution(resolution)
            .trackingId(trackingId)
            .start()

    private fun getLocationData(context: Context): LocationHistoryData {
        val historyString = context.assets.open("location_history_small.txt").use { String(it.readBytes()) }
        return gson.fromJson(historyString, LocationHistoryData::class.java)
    }
}
