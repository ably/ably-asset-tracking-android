package com.ably.tracking.example.subscriber

import android.content.res.ColorStateList
import android.location.Location
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.transition.TransitionManager
import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution
import com.ably.tracking.TrackableState
import com.ably.tracking.connection.Authentication
import com.ably.tracking.connection.ConnectionConfiguration
import com.ably.tracking.logging.LogHandler
import com.ably.tracking.logging.LogLevel
import com.ably.tracking.subscriber.Subscriber
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.asset_information_view.*
import kotlinx.android.synthetic.main.trackable_input_controls_view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber

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
        setTrackableIdEditTextListener()
        setupTrackableInputAction()

        startButton.setOnClickListener {
            if (subscriber == null) {
                startSubscribing()
            } else {
                stopSubscribing()
            }
        }
    }

    private fun setTrackableIdEditTextListener() {
        trackableIdEditText.addTextChangedListener { trackableId ->
            trackableId?.trim()?.let { changeStartButtonColor(it.isNotEmpty()) }
        }
    }

    private fun setupTrackableInputAction() {
        trackableIdEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                startSubscribing()
                true
            } else {
                false
            }
        }
    }

    private fun prepareMap() {
        (mapFragment as SupportMapFragment).let {
            it.getMapAsync { map ->
                map.uiSettings.isZoomControlsEnabled = true
                googleMap = map
            }
        }
    }

    private fun startSubscribing() {
        trackableIdEditText.text.toString().trim().let { trackableId ->
            if (trackableId.isNotEmpty()) {
                googleMap?.clear()
                googleMap?.setOnCameraIdleListener { updateResolutionBasedOnZoomLevel() }
                showAssetInformation()
                trackableIdEditText.isEnabled = false
                createAndStartAssetSubscriber(trackableId)
                updateResolutionInfo(resolution)
                changeStartButtonText(true)
            } else {
                showToast("Insert trackable ID")
            }
        }
    }

    private fun createAndStartAssetSubscriber(trackableId: String) {
        scope.launch {
            subscriber = Subscriber.subscribers()
                .connection(ConnectionConfiguration(Authentication.basic(CLIENT_ID, ABLY_API_KEY)))
                .trackingId(trackableId)
                .resolution(resolution)
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
        resolutionAccuracyTextView.text = resolution.accuracy.name.toLowerCase().capitalize()
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
        val textColorId = when (trackableState) {
            is TrackableState.Online -> R.color.black
            is TrackableState.Offline -> R.color.mid_grey
            is TrackableState.Failed -> R.color.black
        }
        val backgroundColorId = when (trackableState) {
            is TrackableState.Online -> R.color.asset_online
            is TrackableState.Offline -> R.color.asset_offline
            is TrackableState.Failed -> R.color.asset_failed
        }
        assetStateTextView.text = getString(textId)
        assetStateTextView.setTextColor(ContextCompat.getColor(this, textColorId))
        assetStateTextView.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, backgroundColorId))
    }

    private fun stopSubscribing() {
        googleMap?.setOnCameraIdleListener { }
        clearResolutionInfo()
        hideAssetInformation()
        trackableIdEditText.isEnabled = true
        scope.launch {
            try {
                subscriber?.stop()
                subscriber = null
                changeStartButtonText(false)
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
                        val cameraPosition = CameraUpdateFactory.newLatLng(position)
                        currentMarker.setIcon(getMarkerIcon(location.bearing))
                        if (animationSwitch.isChecked) {
                            animateCamera(cameraPosition)
                            animateMarkerMovement(currentMarker, position)
                        } else {
                            moveCamera(cameraPosition)
                            currentMarker.position = position
                        }
                    }
                }
            }
        }
    }

    private fun getMarkerIcon(bearing: Float) =
        BitmapDescriptorFactory.fromResource(getMarkerResourceIdByBearing(bearing))

    private fun changeStartButtonText(isSubscribing: Boolean) {
        if (isSubscribing) {
            startButton.text = getString(R.string.start_button_working)
        } else {
            startButton.text = getString(R.string.start_button_ready)
        }
    }

    private fun changeStartButtonColor(isActive: Boolean) {
        if (isActive) {
            startButton.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.button_active))
            startButton.setTextColor(ContextCompat.getColor(this, R.color.white))
        } else {
            startButton.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.button_inactive))
            startButton.setTextColor(ContextCompat.getColor(this, R.color.mid_grey))
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showAssetInformation() {
        animateAssetInformationVisibility(true)
        draggingAreaView.visibility = View.VISIBLE
    }

    private fun hideAssetInformation() {
        animateAssetInformationVisibility(false)
        draggingAreaView.visibility = View.GONE
    }

    private fun animateAssetInformationVisibility(isShowing: Boolean) {
        TransitionManager.beginDelayedTransition(rootContainer)
        ConstraintSet().apply {
            clone(rootContainer)
            setVisibility(assetInformationContainer.id, if (isShowing) ConstraintSet.VISIBLE else ConstraintSet.GONE)
            applyTo(rootContainer)
        }
    }
}
