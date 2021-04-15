package com.ably.tracking.example.publisher

import android.Manifest
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import com.ably.tracking.Accuracy
import com.ably.tracking.connection.ConnectionConfigurationKey
import com.ably.tracking.Resolution
import com.ably.tracking.publisher.DefaultResolutionPolicyFactory
import com.ably.tracking.publisher.LocationHistoryData
import com.ably.tracking.publisher.LocationSource
import com.ably.tracking.publisher.MapConfiguration
import com.ably.tracking.publisher.Publisher
import com.ably.tracking.publisher.RoutingProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

private const val MAPBOX_ACCESS_TOKEN = BuildConfig.MAPBOX_ACCESS_TOKEN
private const val CLIENT_ID = "<INSERT_CLIENT_ID_HERE>"
private const val ABLY_API_KEY = BuildConfig.ABLY_API_KEY

class PublisherService : Service() {
    // SupervisorJob() is used to keep the scope working after any of its children fail
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val DEFAULT_RESOLUTION = Resolution(Accuracy.MINIMUM, 1000L, 1.0)
    private val NOTIFICATION_ID = 5235
    private val binder = Binder()
    var publisher: Publisher? = null
    private lateinit var appPreferences: AppPreferences

    override fun onCreate() {
        super.onCreate()
        appPreferences = AppPreferences(this) // TODO - Add some DI (Koin)?
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Asset Tracking")
            .setContentText("Publisher is working")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        return START_NOT_STICKY
    }

    inner class Binder : android.os.Binder() {
        fun getService(): PublisherService = this@PublisherService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    /**
     * Creates and starts the [Publisher].
     */
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    fun startPublisher(
        defaultResolution: Resolution = DEFAULT_RESOLUTION,
        locationSource: LocationSource? = null
    ) {
        publisher = Publisher.publishers()
            .connection(ConnectionConfigurationKey.create(ABLY_API_KEY, CLIENT_ID))
            .map(MapConfiguration(MAPBOX_ACCESS_TOKEN))
            .locationSource(locationSource)
            .resolutionPolicy(DefaultResolutionPolicyFactory(defaultResolution, this))
            .androidContext(this)
            .profile(RoutingProfile.DRIVING)
            .start().apply {
                locationHistory
                    .onEach { uploadLocationHistoryData(it) }
                    .launchIn(scope)
            }
    }

    private fun uploadLocationHistoryData(historyData: LocationHistoryData) {
        if (getLocationSourceType() == LocationSourceType.PHONE) {
            S3Helper.uploadHistoryData(
                this,
                historyData
            ) { showToast("S3 not initialized - cannot upload history data") }
        }
    }

    private fun getLocationSourceType() =
        when (appPreferences.getLocationSource()) {
            getString(R.string.location_source_ably) -> LocationSourceType.ABLY
            getString(R.string.location_source_s3) -> LocationSourceType.S3
            else -> LocationSourceType.PHONE
        }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

enum class LocationSourceType { PHONE, ABLY, S3 }
