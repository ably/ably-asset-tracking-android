package com.ably.tracking.example.publisher

import android.app.Activity
import com.ably.tracking.Accuracy
import com.ably.tracking.ConnectionConfiguration
import com.ably.tracking.Resolution
import com.ably.tracking.publisher.DefaultProximity
import com.ably.tracking.publisher.DefaultResolutionConstraints
import com.ably.tracking.publisher.DefaultResolutionSet
import com.ably.tracking.publisher.MapConfiguration
import com.ably.tracking.publisher.Publisher
import com.ably.tracking.publisher.RoutingProfile
import com.ably.tracking.publisher.Trackable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// PLACEHOLDERS:

val ABLY_API_KEY = ""
val CLIENT_ID = ""
val MAPBOX_ACCESS_TOKEN = ""

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
            .connection(ConnectionConfiguration(ABLY_API_KEY, CLIENT_ID)) // provide Ably configuration with credentials
            .map(MapConfiguration(MAPBOX_ACCESS_TOKEN)) // provide Mapbox configuration with credentials
            .androidContext(this) // provide Android runtime context
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
                // TODO handle asset tracking started successfully
                // it's safe to update the UI directly here (see [scope])
            } catch (exception: Exception) {
                // TODO handle asset tracking could not be started
            }
        }
    }
}
