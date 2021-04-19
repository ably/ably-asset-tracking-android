package com.ably.tracking.example.subscriber

import com.ably.tracking.Accuracy
import com.ably.tracking.connection.Authentication
import com.ably.tracking.Resolution
import com.ably.tracking.connection.ConnectionConfiguration
import com.ably.tracking.subscriber.Subscriber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

// PLACEHOLDERS:

val ABLY_API_KEY = ""
val CLIENT_ID = ""

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

    // Request a different resolution when needed.
    scope.launch {
        try {
            subscriber.resolutionPreference(
                Resolution(Accuracy.MAXIMUM, desiredInterval = 100L, minimumDisplacement = 2.0)
            )
            // TODO change request submitted successfully
        } catch (exception: Exception) {
            // TODO change request could not be submitted
        }
    }
}
