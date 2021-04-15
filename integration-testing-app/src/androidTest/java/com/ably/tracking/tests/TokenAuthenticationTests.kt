package com.ably.tracking.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ably.tracking.connection.TokenAuthenticationConfiguration
import com.ably.tracking.connection.TokenRequest
import com.ably.tracking.publisher.Trackable
import com.ably.tracking.subscriber.Subscriber
import com.ably.tracking.test.common.UnitExpectation
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
class TokenAuthenticationTests {
    private val ably = AblyRealtime(ClientOptions(ABLY_API_KEY).apply { autoConnect = false })

    @Test
    fun tokenAuthenticationShouldCreateWorkingConnectionBetweenPublisherAndSubscriber() {
        // given
        val subscriberReceivedLocationUpdateExpectation = UnitExpectation("subscriber received a location update")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val trackableId = UUID.randomUUID().toString()
        val scope = CoroutineScope(Dispatchers.Default)

        // when
        // create subscriber and publisher
        var subscriber: Subscriber
        runBlocking {
            subscriber = createAndStartSubscriber(
                trackableId,
                connectionConfiguration = createTokenAuthenticationConfiguration(ably)
            )
        }

        val publisher = createAndStartPublisher(
            context,
            connectionConfiguration = createTokenAuthenticationConfiguration(ably)
        )

        // listen for location updates
        subscriber.locations
            .onEach { subscriberReceivedLocationUpdateExpectation.fulfill() }
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

    private fun createTokenAuthenticationConfiguration(ably: AblyRealtime): TokenAuthenticationConfiguration =
        TokenAuthenticationConfiguration.create(
            { requestParameters ->
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
                TokenRequest(
                    ttl = ablyTokenRequest.ttl,
                    capability = ablyTokenRequest.capability,
                    clientId = ablyTokenRequest.clientId,
                    timestamp = ablyTokenRequest.timestamp,
                    keyName = ablyTokenRequest.keyName,
                    nonce = ablyTokenRequest.nonce,
                    mac = ablyTokenRequest.mac
                )
            },
            CLIENT_ID
        )
}
