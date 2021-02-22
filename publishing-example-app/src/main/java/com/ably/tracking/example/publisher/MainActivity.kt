package com.ably.tracking.example.publisher

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.ably.tracking.Accuracy
import com.ably.tracking.AssetStatus
import com.ably.tracking.Resolution
import com.ably.tracking.publisher.DefaultProximity
import com.ably.tracking.publisher.DefaultResolutionConstraints
import com.ably.tracking.publisher.DefaultResolutionSet
import com.ably.tracking.publisher.LocationHistoryData
import com.ably.tracking.publisher.LocationSource
import com.ably.tracking.publisher.LocationSourceAbly
import com.ably.tracking.publisher.LocationSourceRaw
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

class MainActivity : PublisherServiceActivity() {
    private lateinit var appPreferences: AppPreferences

    // SupervisorJob() is used to keep the scope working after any of its children fail
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onPublisherServiceConnected(publisherService: PublisherService) {
        if (publisherService.publisher == null) {
            startTracking()
        } else {
            changeNavigationButtonState(true)
            publisherService.publisher?.let { publisher ->
                publisher.active?.id?.let { trackableId ->
                    trackingIdEditText.setText(trackableId)
                    publisher.getAssetStatus(trackableId)
                        ?.onEach { updateAssetStatusInfo(it) }
                        ?.launchIn(scope)
                }
                publisher.locations
                    .onEach { updateLocationInfo(it.location) }
                    .launchIn(scope)
            }
        }
    }

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
            if (publisherService == null) {
                startAndBindPublisherService()
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
    private fun createAndStartAssetPublisher(trackingId: String, historyData: LocationHistoryData? = null) {
        clearLocationInfo()
        startPublisher(
            defaultResolution = Resolution(Accuracy.MINIMUM, 1000L, 1.0),
            locationSource = createLocationSource(historyData)
        )
        publisherService?.publisher?.apply {
            scope.launch {
                try {
                    val assetStatus = track(
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
                    assetStatus
                        .onEach { updateAssetStatusInfo(it) }
                        .launchIn(scope)
                } catch (exception: Exception) {
                    showToast("Error when tracking asset")
                    stopTracking()
                }
            }
            locations
                .onEach { updateLocationInfo(it.location) }
                .launchIn(scope)
            locationHistory
                .onEach { uploadLocationHistoryData(it) }
                .launchIn(scope)
        }
        changeNavigationButtonState(true)
    }

    private fun downloadLocationHistoryData(onHistoryDataDownloaded: (historyData: LocationHistoryData) -> Unit) {
        S3Helper.downloadHistoryData(
            this,
            appPreferences.getS3File(),
            onHistoryDataDownloaded = { onHistoryDataDownloaded(it) },
            onUninitialized = { showToast("S3 not initialized - cannot download history data") }
        )
    }

    private fun createLocationSource(historyData: LocationHistoryData? = null): LocationSource? =
        when (getLocationSourceType()) {
            LocationSourceType.ABLY -> LocationSourceAbly.create(appPreferences.getSimulationChannel())
            LocationSourceType.S3 -> LocationSourceRaw.create(historyData!!)
            LocationSourceType.PHONE -> null
        }

    private fun uploadLocationHistoryData(historyData: LocationHistoryData) {
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
                publisherService?.publisher?.stop()
                publisherService?.publisher = null
                changeNavigationButtonState(false)
            } catch (e: Exception) {
                // TODO check Result (it) for failure and report accordingly
            }
        }
        stopPublisherService()
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

    private fun updateAssetStatusInfo(status: AssetStatus) {
        val textId: Int
        val colorId: Int
        when (status) {
            is AssetStatus.Online -> {
                textId = R.string.online
                colorId = R.color.asset_status_online
            }
            is AssetStatus.Offline -> {
                textId = R.string.offline
                colorId = R.color.asset_status_offline
            }
            is AssetStatus.Failed -> {
                textId = R.string.failed
                colorId = R.color.asset_status_failed
            }
        }
        assetStatusValueTextView.text = getString(textId)
        ImageViewCompat.setImageTintList(
            assetStatusImageView,
            ColorStateList.valueOf(ContextCompat.getColor(this, colorId))
        )
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
