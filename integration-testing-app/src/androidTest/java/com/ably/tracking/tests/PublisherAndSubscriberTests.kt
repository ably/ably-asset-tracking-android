package com.ably.tracking.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ably.tracking.LocationUpdate
import com.ably.tracking.TrackableState
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.subscriber.BuildConfig
import com.ably.tracking.test.android.common.BooleanExpectation
import com.ably.tracking.test.android.common.Logging
import com.ably.tracking.test.android.common.Logging.testLogD
import com.ably.tracking.test.android.common.UnitExpectation
import com.ably.tracking.test.android.common.awaitSubscriberPresent
import com.google.common.truth.Truth.assertThat
import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.types.ClientOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class PublisherAndSubscriberTests {
    @Test
    fun createAndStartPublisherAndSubscriberAndWaitUntilDataEnds() = runTest {
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
        val fakePublisher = FakePublisher(trackableId)
        fakePublisher.enterPresence()

        val subscriber = createAndStartSubscriber(trackableId)
        subscriber.awaitOnline()

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

        try {
            publisher.track(Trackable(trackableId))
            testLogD("track success")
            trackExpectation.fulfill(true)
        } catch (e: Exception) {
            testLogD("track failed")
            trackExpectation.fulfill(false)
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

        /*
            Wait for everything to have been emitted onto the publisher locations channel,
            as this happens on the same coroutine scope as, but outside of, the worker queue.
         */
        withTimeout(10000) {
            while (publishedLocations.size < receivedLocations.size) {
                delay(100)
            }
        }

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
    fun shouldSendRawLocationsWhenTheyAreEnabled() = runTest {
        // given
        var hasNotReceivedLocationUpdate = true
        val subscriberReceivedLocationUpdateExpectation =
            UnitExpectation("subscriber received a location update")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val trackableId = UUID.randomUUID().toString()
        val scope = CoroutineScope(Dispatchers.Default)

        // when
        // create subscriber and publisher
        val subscriber = createAndStartSubscriber(trackableId)
        awaitSubscriberPresent(trackableId)

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
        publisher.track(Trackable(trackableId))

        // await for at least one received location update
        subscriberReceivedLocationUpdateExpectation.await()

        // cleanup
        coroutineScope {
            launch { publisher.stop() }
            launch { subscriber.stop() }
        }

        // then
        subscriberReceivedLocationUpdateExpectation.assertFulfilled()
    }

    @Test
    fun shouldSendCalculatedResolutionsWhenTheyAreEnabled() = runTest {
        // given
        var hasNotReceivedResolution = true
        val subscriberReceivedResolutionExpectation =
            UnitExpectation("subscriber received a publisher resolution")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val trackableId = UUID.randomUUID().toString()
        val scope = CoroutineScope(Dispatchers.Default)

        // when
        // create subscriber and publisher
        val subscriber = createAndStartSubscriber(trackableId)
        awaitSubscriberPresent(trackableId)

        val publisher = createAndStartPublisher(context, sendResolution = true)

        // listen for resolution updates
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
        publisher.track(Trackable(trackableId))

        // await for at least one received publisher resolution
        subscriberReceivedResolutionExpectation.await()

        // cleanup
        coroutineScope {
            launch { publisher.stop() }
            launch { subscriber.stop() }
        }

        // then
        subscriberReceivedResolutionExpectation.assertFulfilled()
    }

    @Test
    fun shouldFailSubscriberWhenItReceivesMalformedMessage() = runTest {
        // given
        val subscriberFailedExpectation = UnitExpectation("subscriber failed")
        val trackableId = UUID.randomUUID().toString()
        val scope = CoroutineScope(Dispatchers.Default)

        val fakePublisher = FakePublisher(trackableId)
        fakePublisher.enterPresence()

        // when
        val subscriber = createAndStartSubscriber(trackableId)
        subscriber.awaitOnline()

        subscriber.trackableStates
            .onEach {
                if (it is TrackableState.Failed) {
                    subscriberFailedExpectation.fulfill()
                }
            }
            .launchIn(scope)

        // publish malformed (empty) message
        fakePublisher.publish("enhanced", "{}")

        // await
        subscriberFailedExpectation.await()

        // captured before cleanup because currently the subscriber on stop is transitioning the trackable to Offline state, reported here https://github.com/ably/ably-asset-tracking-android/issues/802
        val finalTrackableState = subscriber.trackableStates.value

        // cleanup
        subscriber.stop()

        // then
        subscriberFailedExpectation.assertFulfilled()
        assertThat(finalTrackableState)
            .isInstanceOf(TrackableState.Failed::class.java)
    }

    private suspend fun awaitSubscriberPresent(trackableId: String) {
        try {
            val clientOptions = ClientOptions().apply {
                this.clientId = "PublisherAndSubscriberTests"
                this.key = BuildConfig.ABLY_API_KEY
                this.logHandler = Logging.ablyJavaDebugLogger
            }

            AblyRealtime(clientOptions).awaitSubscriberPresent(trackableId, 10_000)
        } catch (exception: TimeoutCancellationException) {
            testLogD("Awaiting for subscriber presence failed after 10 seconds")
        }
    }
}
