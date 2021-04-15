package com.ably.tracking.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ably.tracking.LocationUpdate
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.subscriber.Subscriber
import com.ably.tracking.test.common.BooleanExpectation
import com.ably.tracking.test.common.UnitExpectation
import com.ably.tracking.test.common.testLogD
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

        // TODO replace this sleep with an await on an expectation that the subscriber is ready (connected and subscribed)
        Thread.sleep(4000)

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
}
