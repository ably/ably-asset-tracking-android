package com.ably.tracking.example.publisher

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.ably.tracking.Accuracy
import com.ably.tracking.ConnectionConfiguration
import com.ably.tracking.ConnectionState
import com.ably.tracking.Resolution
import com.ably.tracking.publisher.DebugConfiguration
import com.ably.tracking.publisher.DefaultProximity
import com.ably.tracking.publisher.DefaultResolutionConstraints
import com.ably.tracking.publisher.DefaultResolutionPolicyFactory
import com.ably.tracking.publisher.DefaultResolutionSet
import com.ably.tracking.publisher.LocationSourceAbly
import com.ably.tracking.publisher.LocationSourceRaw
import com.ably.tracking.publisher.MapConfiguration
import com.ably.tracking.publisher.Publisher
import com.ably.tracking.publisher.RoutingProfile
import com.ably.tracking.publisher.Trackable
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import timber.log.Timber

private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
private const val REQUEST_LOCATION_PERMISSION = 1
private const val MAPBOX_ACCESS_TOKEN = BuildConfig.MAPBOX_ACCESS_TOKEN
private const val CLIENT_ID = "<INSERT_CLIENT_ID_HERE>"
private const val ABLY_API_KEY = BuildConfig.ABLY_API_KEY

class MainActivity : AppCompatActivity() {
    private var publisher: Publisher? = null
    private lateinit var appPreferences: AppPreferences
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("Hello via Timber")
        setContentView(R.layout.activity_main)
        appPreferences = AppPreferences(this)
        updateLocationSourceMethodInfo()

        requestLocationPermission()

        settingsFab.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        startNavigationButton.setOnClickListener {
            if (publisher == null) {
                startTracking()
            } else {
                stopTracking()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        updateLocationSourceMethodInfo()
    }

    private fun updateLocationSourceMethodInfo() {
        locationSourceMethodTextView.text = appPreferences.getLocationSource()
    }

    private fun requestLocationPermission() {
        if (!EasyPermissions.hasPermissions(this, *REQUIRED_PERMISSIONS)) {
            EasyPermissions.requestPermissions(
                this,
                "Please grant the location permission",
                REQUEST_LOCATION_PERMISSION,
                *REQUIRED_PERMISSIONS
            )
        }
    }

    @AfterPermissionGranted(REQUEST_LOCATION_PERMISSION)
    fun onLocationPermissionGranted() {
        showToast("Permission granted")
    }

    // Lint doesn't detect that we're checking for required permissions in a separate function
    @SuppressLint("MissingPermission")
    private fun startTracking() {
        if (hasFineOrCoarseLocationPermissionGranted(this)) {
            trackingIdEditText.text.toString().trim().let { trackingId ->
                if (trackingId.isNotEmpty()) {
                    if (getLocationSourceType() == LocationSourceType.S3) {
                        downloadLocationHistoryData { createAndStartAssetPublisher(trackingId, it) }
                    } else {
                        createAndStartAssetPublisher(trackingId)
                    }
                } else {
                    showToast("Insert tracking ID")
                }
            }
        } else {
            requestLocationPermission()
        }
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    private fun createAndStartAssetPublisher(trackingId: String, historyData: String? = null) {
        clearLocationInfo()
        publisher = Publisher.publishers()
            .connection(ConnectionConfiguration(ABLY_API_KEY, CLIENT_ID))
            .map(MapConfiguration(MAPBOX_ACCESS_TOKEN))
            .debug(createDebugConfiguration(historyData))
            .resolutionPolicy(DefaultResolutionPolicyFactory(Resolution(Accuracy.MINIMUM, 1000L, 1.0), this))
            .androidContext(this)
            .profile(RoutingProfile.DRIVING)
            .start()
            .apply {
                scope.launch {
                    try {
                        track(
                            Trackable(
                                trackingId,
                                constraints = DefaultResolutionConstraints(
                                    DefaultResolutionSet(
                                        Resolution(
                                            Accuracy.BALANCED,
                                            desiredInterval = 1000L,
                                            minimumDisplacement = 1.0
                                        )
                                    ),
                                    DefaultProximity(spatial = 1.0),
                                    batteryLevelThreshold = 10.0f,
                                    lowBatteryMultiplier = 2.0f
                                )
                            )
                        )
                    } catch (exception: Exception) {
                        showToast("Error when tracking asset")
                        stopTracking()
                    }
                }
                connectionStates
                    .onEach { updateAblyStateInfo(it.state) }
                    .launchIn(scope)
                locations
                    .onEach { updateLocationInfo(it.location) }
                    .launchIn(scope)
            }
        changeNavigationButtonState(true)
    }

    private fun downloadLocationHistoryData(onHistoryDataDownloaded: (historyData: String) -> Unit) {
        S3Helper.downloadHistoryData(
            this,
            appPreferences.getS3File(),
            onHistoryDataDownloaded = { onHistoryDataDownloaded(it) },
            onUninitialized = { showToast("S3 not initialized - cannot download history data") }
        )
    }

    private fun createDebugConfiguration(historyData: String? = null): DebugConfiguration {
        return DebugConfiguration(
            locationSource = when (getLocationSourceType()) {
                LocationSourceType.ABLY -> LocationSourceAbly(appPreferences.getSimulationChannel())
                LocationSourceType.S3 -> LocationSourceRaw(historyData!!)
                LocationSourceType.PHONE -> null
            },
            locationHistoryHandler = { uploadLocationHistoryData(it) }
        )
    }

    private fun uploadLocationHistoryData(historyData: String) {
        if (getLocationSourceType() == LocationSourceType.PHONE) {
            S3Helper.uploadHistoryData(
                this,
                historyData
            ) { showToast("S3 not initialized - cannot upload history data") }
        }
    }

    private fun stopTracking() {
        scope.launch {
            try {
                publisher?.stop()
                publisher = null
                changeNavigationButtonState(false)
            } catch (e: Exception) {
                // TODO check Result (it) for failure and report accordingly
            }
        }
    }

    private fun updateLocationInfo(location: Location) {
        val lat = location.latitude.toString()
        val lon = location.longitude.toString()
        val bearing = location.bearing.toString()
        latitudeValueTextView.text = if (lat.length > 7) lat.substring(0, 7) else lat
        longitudeValueTextView.text = if (lon.length > 7) lon.substring(0, 7) else lon
        bearingValueTextView.text = if (bearing.length > 7) bearing.substring(0, 7) else bearing
    }

    private fun clearLocationInfo() {
        latitudeValueTextView.text = ""
        longitudeValueTextView.text = ""
        bearingValueTextView.text = ""
    }

    private fun updateAblyStateInfo(state: ConnectionState) {
        val isAblyConnected = state == ConnectionState.CONNECTED
        changeAblyStatusInfo(isAblyConnected)
    }

    private fun changeAblyStatusInfo(isConnected: Boolean) {
        if (isConnected) {
            ablyConnectionStatusValueTextView.text = getString(R.string.online)
            ImageViewCompat.setImageTintList(
                ablyConnectionStatusImageView,
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ably_status_online))
            )
        } else {
            ablyConnectionStatusValueTextView.text = getString(R.string.offline)
            ImageViewCompat.setImageTintList(
                ablyConnectionStatusImageView,
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ably_status_offline))
            )
        }
    }

    private fun changeNavigationButtonState(isPublishing: Boolean) {
        if (isPublishing) {
            startNavigationButton.text = getString(R.string.navigation_button_working)
            startNavigationButton.setBackgroundResource(R.drawable.rounded_working)
        } else {
            startNavigationButton.text = getString(R.string.navigation_button_ready)
            startNavigationButton.setBackgroundResource(R.drawable.rounded_ready)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    enum class LocationSourceType { PHONE, ABLY, S3 }

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
