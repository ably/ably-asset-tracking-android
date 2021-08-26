package com.ably.tracking.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ably.tracking.publisher.BuildConfig
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.test.android.common.UnitExpectation
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.realtime.ChannelState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * This test assumes that this integration testing app has been built with obfuscation
 * enabled for the Ably Asset Tracking classes with a proguard rule something like:
 *
 *     -keep,allowobfuscation class com.ably.tracking.** { *; }
 *
 * If tests in this suite fail then it may be a sign that obfuscation is affecting the
 * serialisation of messages being sent over the Ably network. See
 * [ably/ably-asset-tracking-android#396](https://github.com/ably/ably-asset-tracking-android/issues/396)
 * for full details.
 */
@RunWith(AndroidJUnit4::class)
class ObfuscationTest {
    private val gson = Gson()

    @Test
    fun messagesFormatShouldNotBeAffectedByObfuscation() {
        // given
        var hasNotReceivedLocationUpdate = true
        val receivedLocationUpdateExpectation = UnitExpectation("received a location update")
        val ablyConnectionReadyExpectation = UnitExpectation("ably connected")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val trackableId = UUID.randomUUID().toString()
        var receivedEnhancedMessageJson: JsonObject? = null

        // create Ably connection
        val ably = AblyRealtime(BuildConfig.ABLY_API_KEY)
        val channel = ably.channels.get("tracking:$trackableId")

        // listen for location updates
        channel.subscribe("enhanced") {
            // UnitExpectation throws an error if it's fulfilled more than once so we need to have this check
            if (hasNotReceivedLocationUpdate) {
                hasNotReceivedLocationUpdate = false
                val dataString = it.data as String
                receivedEnhancedMessageJson = gson.fromJson(dataString, JsonObject::class.java)
                receivedLocationUpdateExpectation.fulfill()
            }
        }

        // await for Ably connection to be ready to receive location updates
        channel.on(ChannelState.attached) {
            ablyConnectionReadyExpectation.fulfill()
        }
        ablyConnectionReadyExpectation.await()

        // create publisher
        val publisher = createAndStartPublisher(context)

        // start publishing location updates
        runBlocking {
            publisher.track(Trackable(trackableId))
        }

        // await for at least one received location update
        receivedLocationUpdateExpectation.await()

        // cleanup
        runBlocking {
            publisher.stop()
        }
        ably.close()

        // then
        receivedLocationUpdateExpectation.assertFulfilled()
        ablyConnectionReadyExpectation.assertFulfilled()
        assertNotNull(receivedEnhancedMessageJson)
        receivedEnhancedMessageJson?.let { messageJson ->
            assertTrue(messageJson.has("location"))
            assertTrue(messageJson.has("skippedLocations"))
            assertTrue(messageJson.has("intermediateLocations"))
            assertTrue(messageJson.has("type"))
            val locationJson = messageJson.get("location").asJsonObject
            assertTrue(locationJson.has("type"))
            assertTrue(locationJson.has("geometry"))
            assertTrue(locationJson.has("properties"))
            val geometryJson = locationJson.get("geometry").asJsonObject
            assertTrue(geometryJson.has("type"))
            assertTrue(geometryJson.has("coordinates"))
            val propertiesJson = locationJson.get("properties").asJsonObject
            assertTrue(propertiesJson.has("accuracyHorizontal"))
            assertTrue(propertiesJson.has("bearing"))
            assertTrue(propertiesJson.has("speed"))
            assertTrue(propertiesJson.has("time"))
        }
    }
}
