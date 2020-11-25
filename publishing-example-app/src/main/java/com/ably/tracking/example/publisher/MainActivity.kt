package com.ably.tracking.example.publisher

import android.Manifest
import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.ably.tracking.publisher.AblyConfiguration
import com.ably.tracking.publisher.AssetPublisher
import com.ably.tracking.publisher.DebugConfiguration
import com.ably.tracking.publisher.LocationSourceAbly
import com.ably.tracking.publisher.MapConfiguration
import io.ably.lib.realtime.ConnectionState
import io.ably.lib.realtime.ConnectionStateListener
import kotlinx.android.synthetic.main.activity_main.*
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import timber.log.Timber

private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
private const val REQUEST_LOCATION_PERMISSION = 1
private const val MAP_KEY = "<INSERT_MAP_KEY_HERE>"
private const val CLIENT_ID = "<INSERT_CLIENT_ID_HERE>"
private const val ABLY_KEY = "<INSERT_ABLY_KEY_HERE>"

class MainActivity : AppCompatActivity() {
    private var assetPublisher: AssetPublisher? = null
    private lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("Hello via Timber")
        setContentView(R.layout.activity_main)
        appPreferences = AppPreferences(this)
        updateLocationSourceMethodInfo()

        requestLocationPermission()

        settingsFab.setOnClickListener {
            TODO("Show settings screen")
        }

        startNavigationButton.setOnClickListener {
            if (assetPublisher == null) {
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
        Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
    }

    // Lint doesn't detect that we're checking for required permissions in a separate function
    @SuppressLint("MissingPermission")
    private fun startTracking() {
        if (hasFineOrCoarseLocationPermissionGranted(this)) {
            trackingIdEditText.text.toString().trim().let { trackingId ->
                if (trackingId.isNotEmpty()) {
                    clearLocationInfo()
                    createAndStartAssetPublisher(trackingId)
                    changeNavigationButtonState(true)
                } else {
                    Toast.makeText(this, "Insert tracking ID", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            requestLocationPermission()
        }
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    private fun createAndStartAssetPublisher(trackingId: String) {
        assetPublisher = AssetPublisher.publishers()
            .ablyConfig(AblyConfiguration(ABLY_KEY, CLIENT_ID))
            .mapConfig(MapConfiguration(MAP_KEY))
            .debugConfig(createDebugConfiguration())
            .delivery(trackingId)
            .locationUpdatedListener { updateLocationInfo(it) }
            .androidContext(this)
            .start()
    }

    private fun createDebugConfiguration(): DebugConfiguration {
        return DebugConfiguration(
            ablyStateChangeListener = { updateAblyStateInfo(it) },
            locationSource = when (appPreferences.getLocationSource()) {
                getString(R.string.location_source_ably) -> LocationSourceAbly(appPreferences.getSimulationChannel())
                getString(R.string.location_source_s3) -> TODO("Add support for S3")
                else -> null
            }
        )
    }

    private fun stopTracking() {
        assetPublisher?.stop()
        assetPublisher = null

        changeNavigationButtonState(false)
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

    private fun updateAblyStateInfo(state: ConnectionStateListener.ConnectionStateChange) {
        // TODO - Change Ably listener thread to main thread in the SDK
        // https://github.com/ably/ably-asset-tracking-android/issues/22
        runOnUiThread {
            val isAblyConnected = state.current == ConnectionState.connected
            changeAblyStatusInfo(isAblyConnected)
        }
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
}
