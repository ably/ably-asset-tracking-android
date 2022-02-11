package com.ably.tracking.example.publisher

import android.Manifest
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import com.ably.tracking.Resolution
import com.ably.tracking.connection.Authentication
import com.ably.tracking.connection.ConnectionConfiguration
import com.ably.tracking.logging.LogHandler
import com.ably.tracking.logging.LogLevel
import com.ably.tracking.publisher.DefaultResolutionPolicyFactory
import com.ably.tracking.publisher.LocationHistoryData
import com.ably.tracking.publisher.LocationSource
import com.ably.tracking.publisher.MapConfiguration
import com.ably.tracking.publisher.Publisher
import com.ably.tracking.publisher.PublisherNotificationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber

// The public token for the Mapbox SDK. For more details see the README.
private const val MAPBOX_ACCESS_TOKEN = BuildConfig.MAPBOX_ACCESS_TOKEN

// The client ID for the Ably SDK instance.
private const val CLIENT_ID = "<INSERT_CLIENT_ID_HERE>"

// The API KEY for the Ably SDK. For more details see the README.
private const val ABLY_API_KEY = BuildConfig.ABLY_API_KEY

class PublisherService : Service() {
    // SupervisorJob() is used to keep the scope working after any of its children fail
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val NOTIFICATION_ID = 5235
    private lateinit var notification: Notification
    private val binder = Binder()
    var publisher: Publisher? = null
    private lateinit var appPreferences: AppPreferences

    override fun onCreate() {
        super.onCreate()
        appPreferences = AppPreferences.getInstance(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
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

    override fun onDestroy() {
        // We want to be sure that after the service is stopped the publisher is stopped too.
        // Otherwise we could end up with multiple active publishers.
        scope.launch { publisher?.stop() }
        super.onDestroy()
    }

    val isPublisherStarted: Boolean
        get() = publisher != null

    /**
     * Creates and starts the [Publisher].
     */
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    fun startPublisher(locationSource: LocationSource? = null) {
        if (!isPublisherStarted) {
            publisher = createPublisher(locationSource).apply {
                locationHistory
                    .onEach { uploadLocationHistoryData(it) }
                    .launchIn(scope)
            }
        }
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    private fun createPublisher(locationSource: LocationSource?): Publisher =
        Publisher.publishers()
            .connection(ConnectionConfiguration(Authentication.basic(CLIENT_ID, ABLY_API_KEY)))
            .map(MapConfiguration(MAPBOX_ACCESS_TOKEN))
            .locationSource(locationSource)
            .resolutionPolicy(DefaultResolutionPolicyFactory(createDefaultResolution(), this))
            .androidContext(this)
            .profile(appPreferences.getRoutingProfile().toAssetTracking())
            .logHandler(object : LogHandler {
                override fun logMessage(level: LogLevel, message: String, throwable: Throwable?) {
                    when (level) {
                        LogLevel.VERBOSE -> Timber.v(throwable, message)
                        LogLevel.INFO -> Timber.i(throwable, message)
                        LogLevel.DEBUG -> Timber.d(throwable, message)
                        LogLevel.WARN -> Timber.w(throwable, message)
                        LogLevel.ERROR -> Timber.e(throwable, message)
                    }
                }
            })
            .backgroundTrackingNotificationProvider(
                object : PublisherNotificationProvider {
                    override fun getNotification(): Notification = notification
                },
                NOTIFICATION_ID
            )
            .rawLocations(appPreferences.shouldSendRawLocations())
            .sendResolution(appPreferences.shouldSendResolution())
            .predictions(appPreferences.shouldEnablePredictions())
            .constantLocationEngineResolution(createConstantLocationEngineResolution())
            .start()

    private fun createDefaultResolution(): Resolution =
        Resolution(
            appPreferences.getResolutionAccuracy(),
            appPreferences.getResolutionDesiredInterval(),
            appPreferences.getResolutionMinimumDisplacement().toDouble()
        )

    private fun uploadLocationHistoryData(historyData: LocationHistoryData) {
        if (appPreferences.getLocationSource() == LocationSourceType.PHONE) {
            S3Helper.uploadHistoryData(
                this,
                historyData
            ) { showShortToast(R.string.error_s3_not_initialized_history_data_upload_failed) }
        }
    }

    private fun createConstantLocationEngineResolution(): Resolution? =
        if (appPreferences.isConstantLocationEngineResolutionEnabled()) {
            appPreferences.getConstantLocationEngineResolution()
        } else {
            null
        }
}
