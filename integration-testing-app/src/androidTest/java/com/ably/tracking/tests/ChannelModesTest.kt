package com.ably.tracking.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.subscriber.Subscriber
import com.ably.tracking.test.android.common.UnitExpectation
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ChannelModesTest {
    @Test
    fun shouldCreateOnlyOnePublisherAndOneSubscriberConnection() {
        // given
        val trackableId = UUID.randomUUID().toString()

        // when
        testConnectionBetweenPublisherAndSubscriber(trackableId)
        val metricsJson = getChannelMetricsJson(trackableId)

        // then
        Assert.assertEquals(1, metricsJson.get("publishers").asInt)
        Assert.assertEquals(1, metricsJson.get("subscribers").asInt)
    }

    private fun getChannelMetricsJson(trackableId: String): JsonObject {
        val httpClient = OkHttpClient()
        val ablyKeyParts = ABLY_API_KEY.split(":")
        val request = Request.Builder()
            .url("https://rest.ably.io/channels/tracking:$trackableId")
            .header("Authorization", Credentials.basic(ablyKeyParts.first(), ablyKeyParts.last()))
            .build()
        val response = httpClient.newCall(request).execute()
        val responseBodyJson = Gson().fromJson(response.body!!.string(), JsonObject::class.java)
        return responseBodyJson.getAsJsonObject("status")
            .getAsJsonObject("occupancy")
            .getAsJsonObject("metrics")
    }

    private fun testConnectionBetweenPublisherAndSubscriber(trackableId: String) {
        // given
        var hasNotReceivedLocationUpdate = true
        val subscriberReceivedLocationUpdateExpectation = UnitExpectation("subscriber received a location update")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val scope = CoroutineScope(Dispatchers.Default)

        // when
        // create subscriber and publisher
        var subscriber: Subscriber
        runBlocking {
            subscriber = createAndStartSubscriber(trackableId)
        }

        val publisher = createAndStartPublisher(context)

        // listen for location updates
        subscriber.locations
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
}
