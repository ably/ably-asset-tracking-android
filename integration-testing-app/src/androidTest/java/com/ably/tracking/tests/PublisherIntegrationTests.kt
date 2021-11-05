package com.ably.tracking.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.test.android.common.BooleanExpectation
import com.ably.tracking.test.android.common.UnitExpectation
import com.ably.tracking.test.android.common.testLogD
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PublisherIntegrationTests {
    @Test
    fun createAndStartPublisherAndWaitUntilDataEnds() {
        testLogD("##########  PublisherIntegrationTests.createAndStartPublisherAndWaitUntilDataEnds  ##########")

        // given
        testLogD("GIVEN")
        val dataEndedExpectation = UnitExpectation("data ended")
        val trackExpectation = BooleanExpectation("track response")
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // when
        testLogD("WHEN")
        val publisher = createAndStartPublisher(
            context,
            onLocationDataEnded = {
                testLogD("data ended")
                dataEndedExpectation.fulfill()
            }
        )
        runBlocking {
            try {
                publisher.track(Trackable("ID"))
                testLogD("track success")
                trackExpectation.fulfill(true)
            } catch (e: Exception) {
                testLogD("track failed")
                trackExpectation.fulfill(false)
            }
        }

        // await asynchronous events
        testLogD("AWAIT")
        dataEndedExpectation.await()
        trackExpectation.await()

        // cleanup
        testLogD("CLEANUP")
        val stopExpectation = BooleanExpectation("stop response")
        runBlocking {
            try {
                publisher.stop()
                testLogD("stop success")
                stopExpectation.fulfill(true)
            } catch (e: Exception) {
                testLogD("stop failed")
                stopExpectation.fulfill(true)
            }
        }
        stopExpectation.await()

        // then
        testLogD("THEN")
        dataEndedExpectation.assertFulfilled()
        trackExpectation.assertSuccess()
        stopExpectation.assertSuccess()
    }
}
