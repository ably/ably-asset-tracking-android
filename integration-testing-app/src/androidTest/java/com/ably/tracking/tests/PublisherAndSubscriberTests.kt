package com.ably.tracking.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ably.tracking.LocationUpdate
import com.ably.tracking.TrackableState
import com.ably.tracking.annotations.Experimental
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.subscriber.Subscriber
import com.ably.tracking.test.android.common.BooleanExpectation
import com.ably.tracking.test.android.common.UnitExpectation
import com.ably.tracking.test.android.common.testLogD
import io.ably.lib.realtime.AblyRealtime
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

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

        val publisher = createAndStartPublisher(
            context,
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

    @Test
    fun shouldSendRawLocationsWhenTheyAreEnabled() {
        // given
        var hasNotReceivedLocationUpdate = true
        val subscriberReceivedLocationUpdateExpectation = UnitExpectation("subscriber received a location update")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val trackableId = UUID.randomUUID().toString()
        val scope = CoroutineScope(Dispatchers.Default)

        // when
        // create subscriber and publisher
        var subscriber: Subscriber
        runBlocking {
            subscriber = createAndStartSubscriber(trackableId)
        }

        val publisher = createAndStartPublisher(context, rawLocations = true)

        // listen for location updates
        subscriber.rawLocations
            .onEach {
                // UnitExpectation throws an error if it's fulfilled more than once so we need to have this check
                if (hasNotReceivedLocationUpdate) {
                    hasNotReceivedLocationUpdate = false
                    subscriberReceivedLocationUpdateExpectation.fulfill()
                }
            }
            .launchIn(scope)

        // start publishing location updates
        runBlocking {
            publisher.track(Trackable(trackableId))
        }

        // await for at least one received location update
        subscriberReceivedLocationUpdateExpectation.await()

        // cleanup
        runBlocking {
            coroutineScope {
                launch { publisher.stop() }
                launch { subscriber.stop() }
            }
        }

        // then
        subscriberReceivedLocationUpdateExpectation.assertFulfilled()
    }

    @Test
    fun shouldSendCalculatedResolutionsWhenTheyAreEnabled() {
        // given
        var hasNotReceivedResolution = true
        val subscriberReceivedResolutionExpectation = UnitExpectation("subscriber received a publisher resolution")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val trackableId = UUID.randomUUID().toString()
        val scope = CoroutineScope(Dispatchers.Default)

        // when
        // create subscriber and publisher
        var subscriber: Subscriber
        runBlocking {
            subscriber = createAndStartSubscriber(trackableId)
        }

        val publisher = createAndStartPublisher(context, sendResolution = true)

        // listen for location updates
        subscriber.resolutions
            .onEach {
                // UnitExpectation throws an error if it's fulfilled more than once so we need to have this check
                if (hasNotReceivedResolution) {
                    hasNotReceivedResolution = false
                    subscriberReceivedResolutionExpectation.fulfill()
                }
            }
            .launchIn(scope)

        // start publishing location updates
        runBlocking {
            publisher.track(Trackable(trackableId))
        }

        // await for at least one received publisher resolution
        subscriberReceivedResolutionExpectation.await()

        // cleanup
        runBlocking {
            coroutineScope {
                launch { publisher.stop() }
                launch { subscriber.stop() }
            }
        }

        // then
        subscriberReceivedResolutionExpectation.assertFulfilled()
    }

    @Test
    fun shouldFailSubscriberWhenItReceivesMalformedMessage() {
        // given
        val subscriberFailedExpectation = UnitExpectation("subscriber failed")
        val trackableId = UUID.randomUUID().toString()
        val scope = CoroutineScope(Dispatchers.Default)
        val ably = AblyRealtime(ABLY_API_KEY)

        // when
        var subscriber: Subscriber
        runBlocking {
            subscriber = createAndStartSubscriber(trackableId)
        }

        subscriber.trackableStates
            .onEach {
                if (it is TrackableState.Failed) {
                    subscriberFailedExpectation.fulfill()
                }
            }
            .launchIn(scope)

        // publish malformed (empty) message
        ably.channels.get("tracking:$trackableId").publish("enhanced", "{}")

        // await
        subscriberFailedExpectation.await()

        // cleanup
        runBlocking {
            subscriber.stop()
        }

        // then
        subscriberFailedExpectation.assertFulfilled()
    }

    @OptIn(Experimental::class)
    @Test
    fun shouldNotEmitPublisherPresenceFalseIfPublisherIsPresentFromTheStart() {
        // given
        val subscriberEmittedPublisherPresentExpectation = UnitExpectation("subscriber emitted publisher present")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val trackableId = UUID.randomUUID().toString()
        val scope = CoroutineScope(Dispatchers.Default)
        val publisherPresentValues = mutableListOf<Boolean>()

        // when
        // create publisher and start tracking
        val publisher = createAndStartPublisher(context, sendResolution = true)
        runBlocking {
            publisher.track(Trackable(trackableId))
        }

        // create subscriber and listen for publisher presence
        var subscriber: Subscriber
        runBlocking {
            subscriber = createAndStartSubscriber(trackableId)
        }
        subscriber.publisherPresence
            .onEach { isPublisherPresent ->
                publisherPresentValues.add(isPublisherPresent)
                if (isPublisherPresent) {
                    subscriberEmittedPublisherPresentExpectation.fulfill()
                }
            }
            .launchIn(scope)

        // await for publisher present event
        subscriberEmittedPublisherPresentExpectation.await()

        // cleanup
        runBlocking {
            coroutineScope {
                launch { publisher.stop() }
                launch { subscriber.stop() }
            }
        }

        // then
        subscriberEmittedPublisherPresentExpectation.assertFulfilled()
        Assert.assertTrue("first publisherPresence value should be true", publisherPresentValues.first())
    }
}
