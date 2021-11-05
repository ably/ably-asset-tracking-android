package com.ably.tracking.example.publisher

import android.app.Activity
import android.app.Notification
import androidx.core.app.NotificationCompat
import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution
import com.ably.tracking.connection.Authentication
import com.ably.tracking.connection.ConnectionConfiguration
import com.ably.tracking.locationprovider.RoutingProfile
import com.ably.tracking.locationprovider.mapbox.MapConfiguration
import com.ably.tracking.locationprovider.mapbox.MapboxLocationProvider
import com.ably.tracking.locationprovider.mapbox.PublisherNotificationProvider
import com.ably.tracking.publisher.DefaultProximity
import com.ably.tracking.publisher.DefaultResolutionConstraints
import com.ably.tracking.publisher.DefaultResolutionSet
import com.ably.tracking.publisher.Publisher
import com.ably.tracking.publisher.Trackable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// PLACEHOLDERS:

// The API KEY for the Ably SDK. For more details see the README.
val ABLY_API_KEY = ""

// The client ID for the Ably SDK instance.
val CLIENT_ID = ""

// The public token for the Mapbox SDK. For more details see the README.
val MAPBOX_ACCESS_TOKEN = ""

// The ID of the foreground service notification. For more details see https://developer.android.com/guide/components/foreground-services#start.
val NOTIFICATION_ID = 1

class ExampleUsage(
    val trackingId: String
) : Activity() {
    // SupervisorJob() is used to keep the scope working after any of its children fail
    val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    override fun onStart() {
        super.onStart()

        // EXAMPLE SNIPPET FROM HERE, WITH EXCESS INDENT REMOVED:

        // Prepare Resolution Constraints for the Resolution Policy
        val exampleConstraints = DefaultResolutionConstraints(
            DefaultResolutionSet( // this constructor provides one Resolution for all states
                Resolution(
                    accuracy = Accuracy.BALANCED,
                    desiredInterval = 1000L,
                    minimumDisplacement = 1.0
                )
            ),
            proximityThreshold = DefaultProximity(spatial = 1.0),
            batteryLevelThreshold = 10.0f,
            lowBatteryMultiplier = 2.0f
        )

        // Initialise and Start the Publisher
        val publisher = Publisher.publishers() // get the Publisher builder in default state
            .connection(
                ConnectionConfiguration(
                    Authentication.basic(
                        CLIENT_ID,
                        ABLY_API_KEY,
                    )
                )
            )
            .locationProvider(
                MapboxLocationProvider(
                    this,
                    MapConfiguration(MAPBOX_ACCESS_TOKEN),
                    ConnectionConfiguration(Authentication.basic(CLIENT_ID, ABLY_API_KEY)),
                    null,
                    null,
                    object : PublisherNotificationProvider {
                        override fun getNotification(): Notification =
                            NotificationCompat.Builder(this@ExampleUsage, "test-channel")
                                .setContentTitle("Title")
                                .setContentText("Text")
                                .setSmallIcon(R.drawable.aat_logo)
                                .build()
                    },
                    NOTIFICATION_ID
                )
            )
            .profile(RoutingProfile.DRIVING) // provide mode of transportation for better location enhancements
            .start()

        // Start tracking an asset
        scope.launch {
            try {
                publisher.track(
                    Trackable(
                        trackingId, // provide a tracking identifier for the asset
                        constraints = exampleConstraints // provide a set of Resolution Constraints
                    )
                )
                // handle asset tracking started successfully
                // it's safe to update the UI directly here (see [scope])
            } catch (exception: Exception) {
                // handle asset tracking could not be started
            }
        }
    }
}
