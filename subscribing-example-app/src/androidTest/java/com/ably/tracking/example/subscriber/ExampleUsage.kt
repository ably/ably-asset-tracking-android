package com.ably.tracking.example.subscriber

import com.ably.tracking.Accuracy
import com.ably.tracking.ConnectionConfiguration
import com.ably.tracking.FailureResult
import com.ably.tracking.Resolution
import com.ably.tracking.SuccessResult
import com.ably.tracking.subscriber.Subscriber

// PLACEHOLDERS:

val ABLY_API_KEY = ""
val CLIENT_ID = ""

fun exampleUsage(trackingId: String) {
    // EXAMPLE SNIPPET FROM HERE, WITH EXCESS INDENT REMOVED:

    // Initialise and Start the Subscriber
    val subscriber = Subscriber.subscribers() // Get an AssetSubscriber
        .connection(ConnectionConfiguration(ABLY_API_KEY, CLIENT_ID)) // provide Ably configuration with credentials
        .enhancedLocations { } // provide a function to be called when enhanced location updates are received
        .resolution( // request a specific resolution to be considered by the publisher
            Resolution(Accuracy.MAXIMUM, desiredInterval = 1000L, minimumDisplacement = 1.0)
        )
        .trackingId(trackingId) // provide the tracking identifier for the asset that needs to be tracked
        .assetStatus { } // provide a function to be called when the asset changes online/offline status
        .start() // start listening for updates

    // Request a different resolution when needed.
    subscriber.sendChangeRequest(
        Resolution(Accuracy.MAXIMUM, desiredInterval = 100L, minimumDisplacement = 2.0),
        {
            when (it) {
                is SuccessResult -> {
                    // TODO change request submitted successfully
                }
                is FailureResult -> {
                    // TODO change request could not be submitted
                }
            }
        }
    )
}
