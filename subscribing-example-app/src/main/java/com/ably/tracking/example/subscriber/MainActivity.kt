package com.ably.tracking.example.subscriber

import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ably.tracking.Accuracy
import com.ably.tracking.ConnectionConfigurationKey
import com.ably.tracking.Resolution
import com.ably.tracking.TrackableState
import com.ably.tracking.subscriber.Subscriber
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private const val CLIENT_ID = "<INSERT_CLIENT_ID_HERE>"
private const val ABLY_API_KEY = BuildConfig.ABLY_API_KEY
private const val MILLISECONDS_PER_SECOND = 1000L
private const val ZOOM_LEVEL_BUILDINGS = 20F
private const val ZOOM_LEVEL_STREETS = 15F
private const val ZOOM_LEVEL_CITY = 10F
private const val ZOOM_LEVEL_CONTINENT = 5F

class MainActivity : AppCompatActivity() {
    private var subscriber: Subscriber? = null
    private var googleMap: GoogleMap? = null
    private var marker: Marker? = null
    private var resolution: Resolution =
        Resolution(Accuracy.MAXIMUM, desiredInterval = 1000L, minimumDisplacement = 1.0)

    // SupervisorJob() is used to keep the scope working after any of its children fail
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prepareMap()

        startButton.setOnClickListener {
            if (subscriber == null) {
                startSubscribing()
            } else {
                stopSubscribing()
            }
        }
    }

    private fun prepareMap() {
        (mapFragment as SupportMapFragment).let {
            it.getMapAsync { map -> googleMap = map }
        }
    }

    private fun startSubscribing() {
        trackingIdEditText.text.toString().trim().let { trackingId ->
            if (trackingId.isNotEmpty()) {
                googleMap?.clear()
                googleMap?.setOnCameraIdleListener { updateResolutionBasedOnZoomLevel() }
                createAndStartAssetSubscriber(trackingId)
                updateResolutionInfo(resolution)
                changeStartButtonState(true)
            } else {
                showToast("Insert tracking ID")
            }
        }
    }

    private fun createAndStartAssetSubscriber(trackingId: String) {
        scope.launch {
            subscriber = Subscriber.subscribers()
                .connection(ConnectionConfigurationKey(ABLY_API_KEY, CLIENT_ID))
                .trackingId(trackingId)
                .resolution(resolution)
                .start()
                .apply {
                    locations
                        .onEach { showMarkerOnMap(it.location) }
                        .launchIn(scope)
                    trackableStates
                        .onEach { updateAssetState(it) }
                        .launchIn(scope)
                }
        }
    }

    private fun updateResolutionBasedOnZoomLevel() {
        googleMap?.cameraPosition?.zoom?.let {
            val newResolution = getResolutionForZoomLevel(it)
            if (newResolution != resolution) {
                scope.launch { changeResolution(newResolution) }
            }
        }
    }

    private suspend fun changeResolution(newResolution: Resolution) {
        // TODO - is this try/catch the best way to do it? maybe we should return the Result and let clients handle it their way?
        subscriber?.let {
            try {
                it.resolutionPreference(newResolution)
                resolution = newResolution
                updateResolutionInfo(newResolution)
            } catch (exception: Exception) {
                showToast("Changing resolution error")
            }
        }
    }

    private fun getResolutionForZoomLevel(zoomLevel: Float): Resolution =
        when (zoomLevel) {
            in ZOOM_LEVEL_BUILDINGS..Float.MAX_VALUE -> Resolution(
                Accuracy.MAXIMUM, desiredInterval = 1 * MILLISECONDS_PER_SECOND, minimumDisplacement = 1.0
            )
            in ZOOM_LEVEL_STREETS..ZOOM_LEVEL_BUILDINGS -> Resolution(
                Accuracy.HIGH, desiredInterval = 3 * MILLISECONDS_PER_SECOND, minimumDisplacement = 10.0
            )
            in ZOOM_LEVEL_CITY..ZOOM_LEVEL_STREETS -> Resolution(
                Accuracy.BALANCED, desiredInterval = 10 * MILLISECONDS_PER_SECOND, minimumDisplacement = 100.0
            )
            in ZOOM_LEVEL_CONTINENT..ZOOM_LEVEL_CITY -> Resolution(
                Accuracy.LOW, desiredInterval = 60 * MILLISECONDS_PER_SECOND, minimumDisplacement = 5000.0
            )
            else -> Resolution(
                Accuracy.MINIMUM, desiredInterval = 120 * MILLISECONDS_PER_SECOND, minimumDisplacement = 10000.0
            )
        }

    private fun updateResolutionInfo(resolution: Resolution) {
        resolutionAccuracyTextView.text = resolution.accuracy.name
        resolutionDisplacementTextView.text =
            getString(R.string.resolution_minimum_displacement_value, resolution.minimumDisplacement)
        resolutionIntervalTextView.text =
            getString(R.string.resolution_desired_interval_value, resolution.desiredInterval)
    }

    private fun clearResolutionInfo() {
        resolutionAccuracyTextView.text = ""
        resolutionDisplacementTextView.text = ""
        resolutionIntervalTextView.text = ""
    }

    private fun updateAssetState(trackableState: TrackableState) {
        val textId = when (trackableState) {
            is TrackableState.Online -> R.string.asset_status_online
            is TrackableState.Offline -> R.string.asset_status_offline
            is TrackableState.Failed -> R.string.asset_status_failed
        }
        assetStateTextView.text = getString(textId)
    }

    private fun stopSubscribing() {
        googleMap?.setOnCameraIdleListener { }
        clearResolutionInfo()
        scope.launch {
            try {
                subscriber?.stop()
                subscriber = null
                changeStartButtonState(false)
                marker = null
            } catch (exception: Exception) {
                // TODO check Result (it) for failure and report accordingly
            }
        }
    }

    private fun showMarkerOnMap(location: Location) {
        googleMap?.apply {
            LatLng(location.latitude, location.longitude).let { position ->
                marker.let { currentMarker ->
                    if (currentMarker == null) {
                        marker = addMarker(
                            MarkerOptions()
                                .position(position)
                                .icon(getMarkerIcon(location.bearing))
                        )
                        moveCamera(CameraUpdateFactory.newLatLngZoom(position, ZOOM_LEVEL_STREETS))
                    } else {
                        currentMarker.setIcon(getMarkerIcon(location.bearing))
                        if (animationSwitch.isChecked) {
                            animateMarkerMovement(currentMarker, position)
                        } else {
                            currentMarker.position = position
                        }
                    }
                }
            }
        }
    }

    private fun getMarkerIcon(bearing: Float) =
        BitmapDescriptorFactory.fromResource(getMarkerResourceIdByBearing(bearing))

    private fun changeStartButtonState(isSubscribing: Boolean) {
        if (isSubscribing) {
            startButton.text = getString(R.string.start_button_working)
            startButton.setBackgroundResource(R.drawable.rounded_working)
        } else {
            startButton.text = getString(R.string.start_button_ready)
            startButton.setBackgroundResource(R.drawable.rounded_ready)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
