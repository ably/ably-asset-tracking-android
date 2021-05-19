package com.ably.tracking.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ably.tracking.connection.Authentication
import com.ably.tracking.connection.TokenRequest
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.subscriber.Subscriber
import com.ably.tracking.test.android.common.UnitExpectation
import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.rest.Auth
import io.ably.lib.types.ClientOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AuthenticationTests {
    private val ably = AblyRealtime(ClientOptions(ABLY_API_KEY).apply { autoConnect = false })

    @Test
    fun basicAuthenticationShouldCreateWorkingConnectionBetweenPublisherAndSubscriber() {
        testConnectionBetweenPublisherAndSubscriber(createBasicAuthenticationConfiguration())
    }

    @Test
    fun tokenAuthenticationShouldCreateWorkingConnectionBetweenPublisherAndSubscriber() {
        testConnectionBetweenPublisherAndSubscriber(createTokenAuthenticationConfiguration(ably))
    }

    private fun createBasicAuthenticationConfiguration(): Authentication =
        Authentication.basic(CLIENT_ID, ABLY_API_KEY)

    private fun createTokenAuthenticationConfiguration(ably: AblyRealtime): Authentication =
        Authentication.tokenRequest(CLIENT_ID) { requestParameters ->
            // use Ably SDK to create a signed token request (this should normally be done by user auth servers)
            val ablyTokenRequest = ably.auth.createTokenRequest(
                Auth.TokenParams().apply {
                    ttl = requestParameters.ttl
                    capability = requestParameters.capability
                    clientId = requestParameters.clientId
                    timestamp = requestParameters.timestamp
                },
                Auth.AuthOptions(ABLY_API_KEY)
            )

            // map the Ably token request to the Asset Tracking token request
            object : TokenRequest {
                override val keyName: String = ablyTokenRequest.keyName
                override val nonce: String = ablyTokenRequest.nonce
                override val mac: String = ablyTokenRequest.mac
                override val ttl: Long = ablyTokenRequest.ttl
                override val capability: String? = ablyTokenRequest.capability
                override val clientId: String = ablyTokenRequest.clientId
                override val timestamp: Long = ablyTokenRequest.timestamp
            }
        }

    private fun testConnectionBetweenPublisherAndSubscriber(authentication: Authentication) {
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
            subscriber = createAndStartSubscriber(trackableId, authentication = authentication)
        }

        val publisher = createAndStartPublisher(context, authentication = authentication)

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
