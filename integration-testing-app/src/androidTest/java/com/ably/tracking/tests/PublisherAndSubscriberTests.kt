package com.ably.tracking.tests

import android.annotation.SuppressLint
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ably.tracking.Accuracy
import com.ably.tracking.AssetStatusHandler
import com.ably.tracking.ConnectionConfiguration
import com.ably.tracking.Handler
import com.ably.tracking.LocationUpdate
import com.ably.tracking.LocationUpdateHandler
import com.ably.tracking.Resolution
import com.ably.tracking.Result
import com.ably.tracking.publisher.DebugConfiguration
import com.ably.tracking.publisher.DefaultResolutionPolicyFactory
import com.ably.tracking.publisher.LocationSourceRaw
import com.ably.tracking.publisher.MapConfiguration
import com.ably.tracking.publisher.Publisher
import com.ably.tracking.publisher.RoutingProfile
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.subscriber.Subscriber
import com.ably.tracking.test.common.UnitExpectation
import com.ably.tracking.test.common.testLogD
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

private const val MAPBOX_ACCESS_TOKEN = BuildConfig.MAPBOX_ACCESS_TOKEN
private const val CLIENT_ID = "IntegrationTestsClient"
private const val ABLY_API_KEY = BuildConfig.ABLY_API_KEY

@RunWith(AndroidJUnit4::class)
class PublisherAndSubscriberTests {

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
        var result: Result<Unit>? = null

        // when
        val subscriber = createAndStartSubscriber(
            trackableId,
            enhancedLocationHandler = { receivedLocations.add(it) }
        )

        // TODO replace this sleep with an await on an expectation that the subscriber is ready (connected and subscribed)
        Thread.sleep(4000)

        val publisher = createAndStartPublisher(
            context,
            locationData = locationData,
            onLocationDataEnded = {
                dataEndedExpectation.fulfill()
            },
            onLocationUpdated = { publishedLocations.add(it) }
        ).apply {
            track(
                Trackable(trackableId),
                { result = it }
            )
        }

        // await
        dataEndedExpectation.await()

        // TODO replace this sleep with an await on an expectation
        Thread.sleep(6000)

        testLogD("COUNT published ${publishedLocations.size}") // observed 15
        testLogD("COUNT received ${receivedLocations.size}") // observed 12

        // cleanup
        publisher.stop { publisherStoppedExpectation.fulfill() }
        subscriber.stop { subscriberStoppedExpectation.fulfill() }
        publisherStoppedExpectation.await()
        subscriberStoppedExpectation.await()

        // then
        dataEndedExpectation.assertFulfilled()
        publisherStoppedExpectation.assertFulfilled()
        subscriberStoppedExpectation.assertFulfilled()
        Assert.assertTrue("Expected success callback on track.", result?.isSuccess ?: false)
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
        locationData: String,
        onLocationDataEnded: Handler<Unit> = {},
        onLocationUpdated: LocationUpdateHandler = {}
    ) =
        Publisher.publishers()
            .androidContext(context)
            .connection(ConnectionConfiguration(ABLY_API_KEY, CLIENT_ID))
            .map(MapConfiguration(MAPBOX_ACCESS_TOKEN))
            .locations(onLocationUpdated)
            .resolutionPolicy(DefaultResolutionPolicyFactory(resolution, context))
            .profile(RoutingProfile.DRIVING)
            .debug(
                DebugConfiguration(locationSource = LocationSourceRaw(locationData, onLocationDataEnded))
            )
            .start()

    private fun createAndStartSubscriber(
        trackingId: String,
        resolution: Resolution = Resolution(Accuracy.BALANCED, 1L, 0.0),
        assetStatusHandler: AssetStatusHandler = {},
        enhancedLocationHandler: LocationUpdateHandler = {}
    ) =
        Subscriber.subscribers()
            .assetStatus(assetStatusHandler)
            .connection(ConnectionConfiguration(ABLY_API_KEY, CLIENT_ID))
            .enhancedLocations(enhancedLocationHandler)
            .resolution(resolution)
            .trackingId(trackingId)
            .start()

    private fun getLocationData(context: Context) =
        context.assets.open("location_history_small.txt").use { String(it.readBytes()) }
}
