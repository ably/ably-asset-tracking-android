package com.ably.tracking.example.publisher

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.ably.tracking.Accuracy
import com.ably.tracking.ConnectionConfiguration
import com.ably.tracking.Resolution
import com.ably.tracking.publisher.DebugConfiguration
import com.ably.tracking.publisher.DefaultResolutionPolicyFactory
import com.ably.tracking.publisher.MapConfiguration
import com.ably.tracking.publisher.Publisher
import com.ably.tracking.publisher.RoutingProfile

private const val NO_FLAGS = 0
private const val MAPBOX_ACCESS_TOKEN = BuildConfig.MAPBOX_ACCESS_TOKEN
private const val CLIENT_ID = "<INSERT_CLIENT_ID_HERE>"
private const val ABLY_API_KEY = BuildConfig.ABLY_API_KEY

/**
 * The activity to extend when connection to the publisher service is needed.
 */
abstract class PublisherServiceActivity : AppCompatActivity() {
    private val DEFAULT_RESOLUTION = Resolution(Accuracy.MINIMUM, 1000L, 1.0)
    protected var publisherService: PublisherService? = null
    private val publisherServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, serviceBinder: IBinder) {
            (serviceBinder as PublisherService.Binder).getService().let { service ->
                publisherService = service
                onPublisherServiceConnected(service)
            }
        }

        override fun onServiceDisconnected(className: ComponentName) {
            publisherService = null
            onPublisherServiceDisconnected()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindPublisherService()
    }

    override fun onDestroy() {
        unbindPublisherService()
        super.onDestroy()
    }

    /**
     * Creates and starts the [Publisher] from the [PublisherService].
     */
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    protected fun startPublisher(
        defaultResolution: Resolution = DEFAULT_RESOLUTION,
        debugConfiguration: DebugConfiguration? = null
    ) {
        publisherService?.publisher = Publisher.publishers()
            .connection(ConnectionConfiguration(ABLY_API_KEY, CLIENT_ID))
            .map(MapConfiguration(MAPBOX_ACCESS_TOKEN))
            .debug(debugConfiguration)
            .resolutionPolicy(DefaultResolutionPolicyFactory(defaultResolution, this))
            .androidContext(this)
            .profile(RoutingProfile.DRIVING)
            .start()
    }

    /**
     * Called when there's a successful connection with [PublisherService].
     * Should be overwritten in the inheriting class if it wants to perform some actions on the service connect.
     */
    protected open fun onPublisherServiceConnected(publisherService: PublisherService) {

    }

    /**
     * Called when the [PublisherService] is disconnected.
     * Should be overwritten in the inheriting class if it wants to perform some actions on the service disconnect.
     */
    protected open fun onPublisherServiceDisconnected() {

    }

    /**
     * Creates and starts the [PublisherService].
     */
    protected fun startAndBindPublisherService() {
        ContextCompat.startForegroundService(this, createServiceIntent())
        bindPublisherService()
    }

    /**
     * Stops the [PublisherService].
     */
    protected fun stopPublisherService() {
        stopService(createServiceIntent())
    }

    private fun bindPublisherService() {
        bindService(createServiceIntent(), publisherServiceConnection, NO_FLAGS)
    }

    private fun unbindPublisherService() {
        unbindService(publisherServiceConnection)
    }

    private fun createServiceIntent() = Intent(this, PublisherService::class.java)

}
