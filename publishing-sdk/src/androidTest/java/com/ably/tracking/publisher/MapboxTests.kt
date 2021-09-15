package com.ably.tracking.publisher

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ably.tracking.Location
import com.ably.tracking.connection.Authentication
import com.ably.tracking.connection.ConnectionConfiguration
import com.ably.tracking.test.android.common.NOTIFICATION_CHANNEL_ID
import com.ably.tracking.test.android.common.UnitExpectation
import com.ably.tracking.test.android.common.createNotificationChannel
import com.ably.tracking.test.android.common.testLogD
import com.google.gson.Gson
import com.mapbox.module.Mapbox_TripNotificationModuleConfiguration
import com.mapbox.navigation.base.trip.notification.TripNotification
import com.mapbox.navigation.core.MapboxNavigation
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

private const val MAPBOX_ACCESS_TOKEN = BuildConfig.MAPBOX_ACCESS_TOKEN
private const val CLIENT_ID = "IntegrationTestsClient"
private const val ABLY_API_KEY = BuildConfig.ABLY_API_KEY

@RunWith(AndroidJUnit4::class)
class MapboxTests {
    private val gson = Gson()

    @Test
    fun shouldNotThrowErrorWhenMapboxIsStartedAndStoppedWithoutStartingTrip() {
        // given
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        createNotificationChannel(context)
        val mapbox: Mapbox = createMapbox(context)

        // when
        mapbox.stopAndClose()

        // then
    }

    /**
     * This test aims to check if the [Mapbox.setRoute] callbacks are called in the order in which they are invoked.
     *
     * This is important because we want to always use the destination from the most recent active trackable.
     * I've also tried to check if [Mapbox.clearRoute] cancels the ongoing [Mapbox.setRoute] callbacks but it doesn't.
     *
     * While working on this test I've discovered an issue connected probably to creating multiple Mapbox instances.
     * When I was running this test in isolation everything worked fine. However, when I ran the whole test class
     * then this test was randomly failing with a SIGSEGV fatal signal. This probably means some troubles in native C++ code.
     * To show this issue I'm deliberately creating a [MapboxNavigation] instance (normally it's hidden behind the [Mapbox] wrapper)
     * and then stopping it with [MapboxNavigation.onDestroy]. The issue happens randomly which makes it harder to debug.
     * If we introduce a delay between stopping the first instance and starting the second one then the test succeeds.
     */
    @SuppressLint("MissingPermission")
    @Test
    fun routeCallbacksShouldBeCalledInTheSameOrderInWhichTheyWereStarted() {
        // given
        val numberOfRoutes = 3
        val orderedRouteNumbers = (1..numberOfRoutes).toList()
        val startLocation = Location(1.0, 2.0, 3.0, 0f, 0f, 0f, 1000L)
        val callbacksOrder = mutableListOf<Int>()
        val callbackExpectations = orderedRouteNumbers.map { UnitExpectation("$it destination callback called") }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        createNotificationChannel(context)

        // create and stop a MapboxNavigation instance to make the test break (it happens randomly)
        setupMapboxNavigationTripNotification(context)
        val mapboxNavigation =
            MapboxNavigation(MapboxNavigation.defaultNavigationOptionsBuilder(context, MAPBOX_ACCESS_TOKEN).build())
        mapboxNavigation.onDestroy()

        // If we would uncomment this sleep and wait for a 1 second here the test will succeed
        // Thread.sleep(1000)

        // create the Mapbox wrapper class that we want to test
        val mapbox: Mapbox = createMapbox(context)

        // when
        mapbox.startTrip()
        orderedRouteNumbers.forEach { routeNumber ->
            testLogD("Setting route number $routeNumber")
            mapbox.setRoute(
                startLocation,
                Destination(routeNumber.toDouble(), routeNumber.toDouble()),
                RoutingProfile.DRIVING
            ) {
                testLogD("Route $routeNumber set")
                callbacksOrder.add(routeNumber)
                callbackExpectations[routeNumber - 1].fulfill()
            }
        }

        // await asynchronous events
        testLogD("Waiting for setRoute callbacks")
        callbackExpectations.forEach { it.await() }

        // cleanup
        testLogD("Stopping mapbox wrapper")
        mapbox.stopAndClose()

        // then
        Assert.assertEquals("All route callback should be called", numberOfRoutes, callbacksOrder.size)
        Assert.assertEquals(
            "Callbacks should be called in the order of invocation",
            orderedRouteNumbers,
            callbacksOrder
        )
    }

    private fun createMapbox(context: Context) =
        DefaultMapbox(
            context,
            MapConfiguration(MAPBOX_ACCESS_TOKEN),
            ConnectionConfiguration(Authentication.basic(CLIENT_ID, ABLY_API_KEY)),
            LocationSourceRaw.create(getLocationData(context), null),
            null,
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

    private fun getLocationData(context: Context): LocationHistoryData {
        val historyString = context.assets.open("location_history_small.txt").use { String(it.readBytes()) }
        return gson.fromJson(historyString, LocationHistoryData::class.java)
    }

    // Utility function required to create MapboxNavigation in this test file. Normally this is done in the Mapbox wrapper class.
    private fun setupMapboxNavigationTripNotification(context: Context) {
        Mapbox_TripNotificationModuleConfiguration.moduleProvider =
            object : Mapbox_TripNotificationModuleConfiguration.ModuleProvider {
                override fun createTripNotification(): TripNotification =
                    MapboxTripNotification(
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
            }
    }
}
