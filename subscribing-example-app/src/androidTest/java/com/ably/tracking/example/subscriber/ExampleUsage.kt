package com.ably.tracking.example.subscriber

import com.ably.tracking.Accuracy
import com.ably.tracking.connection.Authentication
import com.ably.tracking.Resolution
import com.ably.tracking.annotations.Experimental
import com.ably.tracking.connection.ConnectionConfiguration
import com.ably.tracking.subscriber.Subscriber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking

// PLACEHOLDERS:

// The API KEY for the Ably SDK. For more details see the README.
val ABLY_API_KEY = ""

// The client ID for the Ably SDK instance.
val CLIENT_ID = ""

@OptIn(Experimental::class)
fun exampleUsage(trackingId: String) {
    val scope = CoroutineScope(Dispatchers.Main)
    // EXAMPLE SNIPPET FROM HERE, WITH EXCESS INDENT REMOVED:

    // Initialise and Start the Subscriber
    var subscriber: Subscriber
    runBlocking {
        subscriber = Subscriber.subscribers() // Get an AssetSubscriber
            .connection(
                ConnectionConfiguration(
                    Authentication.basic(
                        CLIENT_ID,
                        ABLY_API_KEY,
                    )
                )
            ) // provide Ably configuration with credentials
            .resolution( // request a specific resolution to be considered by the publisher
                Resolution(Accuracy.MAXIMUM, desiredInterval = 1000L, minimumDisplacement = 1.0)
            )
            .trackingId(trackingId) // provide the tracking identifier for the asset that needs to be tracked
            .start() // start listening for updates
    }

    subscriber.locations
        .onEach {
            // provide a function to be called when enhanced location updates are received
        }
        .launchIn(scope)

    subscriber.trackableStates
        .onEach {
            // provide a function to be called when the asset changes online/offline status
        }
        .launchIn(scope)
    subscriber.publisherPresence
        .onEach {
            // TODO provide a function to be called when the publisher changes online/offline status
        }
        .launchIn(scope)
    // Request a different resolution when needed.
    subscriber.sendResolutionPreference(
        Resolution(
            Accuracy.MAXIMUM,
            desiredInterval = 100L,
            minimumDisplacement = 2.0
        )
    )
}
