package com.ably.tracking.publisher

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution
import com.ably.tracking.connection.Authentication
import com.ably.tracking.connection.ConnectionConfiguration
import com.ably.tracking.connection.TokenRequest
import com.ably.tracking.test.android.common.NOTIFICATION_CHANNEL_ID
import com.google.gson.Gson
import io.ably.lib.realtime.AblyRealtime
import io.ably.lib.rest.Auth
import io.ably.lib.types.ClientOptions
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

private const val MAPBOX_ACCESS_TOKEN = BuildConfig.MAPBOX_ACCESS_TOKEN
private const val CLIENT_ID = "RequestingNewTokenTestClient"
private const val ABLY_API_KEY = BuildConfig.ABLY_API_KEY

@RunWith(AndroidJUnit4::class)
class RequestingNewTokenTest {
    private val ably = AblyRealtime(ClientOptions(ABLY_API_KEY).apply { autoConnect = false })

    @Test
    fun shouldAddTrackableWhenRenewedTokenHasCapabilityForTrackableId() {
        // given
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val trackableId = UUID.randomUUID().toString()
        val authentication = createTokenAuthenticationConfiguration(
            initialEnabledTrackableIds = listOf("xyz"),
            newEnabledTrackableIds = listOf("xyz", trackableId)
        )

        // when
        val publisher = createAndStartPublisher(context, authentication)
        val isOperationSuccessful = addTrackableAndStopPublisher(publisher, trackableId)

        // then
        Assert.assertTrue(isOperationSuccessful)
    }

    @Ignore("this will be fixed elsewhere")
    @Test
    fun shouldNotAddTrackableWhenRenewedTokenDoesNotHaveCapabilityForTrackableId() {
        // given
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val trackableId = UUID.randomUUID().toString()
        val authentication = createTokenAuthenticationConfiguration(
            initialEnabledTrackableIds = listOf("xyz"),
            newEnabledTrackableIds = listOf("xyz")
        )

        // when
        val publisher = createAndStartPublisher(context, authentication)
        val isOperationSuccessful = addTrackableAndStopPublisher(publisher, trackableId)

        // then
        Assert.assertFalse(isOperationSuccessful)
    }

    /**
     * Adds a trackable with the [trackableId] and stops the [publisher].
     * @return true if adding trackable was sucessfull, false otherwise
     */
    private fun addTrackableAndStopPublisher(publisher: Publisher, trackableId: String): Boolean =
        runBlocking {
            try {
                publisher.add(Trackable(trackableId))
                true
            } catch (exception: Exception) {
                false
            } finally {
                publisher.stop()
            }
        }

    private fun createTokenAuthenticationConfiguration(
        initialEnabledTrackableIds: List<String>,
        newEnabledTrackableIds: List<String>,
    ): Authentication {
        var requestedForFirstTime = true
        return Authentication.tokenRequest { requestParameters ->
            // Create different token capabilities when called for the first time and when called next times
            val capabilities = if (requestedForFirstTime) {
                requestedForFirstTime = false
                initialEnabledTrackableIds.asTokenCapabilities()
            } else {
                newEnabledTrackableIds.asTokenCapabilities()
            }

            // use Ably SDK to create a signed token request (this should normally be done by user auth servers)
            val ablyTokenRequest = ably.auth.createTokenRequest(
                Auth.TokenParams().apply {
                    ttl = requestParameters.ttl
                    capability = capabilities
                    clientId = CLIENT_ID
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
    }

    @SuppressLint("MissingPermission")
    private fun createAndStartPublisher(context: Context, authentication: Authentication) =
        Publisher.publishers()
            .androidContext(context)
            .connection(ConnectionConfiguration(authentication))
            .map(MapConfiguration(MAPBOX_ACCESS_TOKEN))
            .resolutionPolicy(DefaultResolutionPolicyFactory(Resolution(Accuracy.BALANCED, 1000L, 0.0), context))
            .locationSource(LocationSourceRaw.create(getLocationData(context)))
            .backgroundTrackingNotificationProvider(
                object : PublisherNotificationProvider {
                    override fun getNotification(): Notification =
                        NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                            .setContentTitle("TEST")
                            .setContentText("Test")
                            .setSmallIcon(R.drawable.aat_logo)
                            .build()
                },
                12345
            )
            .start()

    private fun getLocationData(context: Context): LocationHistoryData {
        val historyString = context.assets.open("location_history_small.txt").use { String(it.readBytes()) }
        return Gson().fromJson(historyString, LocationHistoryData::class.java)
    }

    private fun List<String>.asTokenCapabilities(): String =
        "{${joinToString(separator = ",") { """"tracking:$it":["*"]""" }}}"
}
