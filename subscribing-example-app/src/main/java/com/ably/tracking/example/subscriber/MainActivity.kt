package com.ably.tracking.example.subscriber

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.transition.TransitionManager
import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution
import com.ably.tracking.TrackableState
import com.ably.tracking.annotations.Experimental
import com.ably.tracking.connection.Authentication
import com.ably.tracking.connection.ConnectionConfiguration
import com.ably.tracking.logging.LogHandler
import com.ably.tracking.logging.LogLevel
import com.ably.tracking.subscriber.Subscriber
import com.ably.tracking.ui.animation.CoreLocationAnimator
import com.ably.tracking.ui.animation.LocationAnimator
import com.ably.tracking.ui.animation.Position
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
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

// The client ID for the Ably SDK instance.
private const val CLIENT_ID = "<INSERT_CLIENT_ID_HERE>"

// The API KEY for the Ably SDK. For more details see the README.
private const val ABLY_API_KEY = BuildConfig.ABLY_API_KEY
private const val MILLISECONDS_PER_SECOND = 1000L
private const val ZOOM_LEVEL_BUILDINGS = 20F
private const val ZOOM_LEVEL_STREETS = 15F
private const val ZOOM_LEVEL_CITY = 10F
private const val ZOOM_LEVEL_CONTINENT = 5F

class MainActivity : AppCompatActivity() {
    private var subscriber: Subscriber? = null
    private var googleMap: GoogleMap? = null
    private var enhancedMarker: Marker? = null
    private var enhancedAccuracyCircle: Circle? = null
    private var rawMarker: Marker? = null
    private var rawAccuracyCircle: Circle? = null
    private var resolution: Resolution =
        Resolution(Accuracy.MAXIMUM, desiredInterval = 1000L, minimumDisplacement = 1.0)
    private var locationUpdateIntervalInMilliseconds = resolution.desiredInterval
    private val enhancedLocationAnimator: LocationAnimator = CoreLocationAnimator(
        animationStepsBetweenCameraUpdates = 5,
    )
    private val rawLocationAnimator: LocationAnimator = CoreLocationAnimator()
    private val animationOptionsManager = AnimationOptionsManager()

    // SupervisorJob() is used to keep the scope working after any of its children fail
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prepareMap()
        setTrackableIdEditTextListener()
        setupTrackableInputAction()
        setupLocationMarkerAnimations()
        setupAnimationOptions()

        startButton.setOnClickListener {
            if (subscriber == null) {
                startSubscribing()
            } else {
                stopSubscribing()
            }
        }
    }

    private fun setupAnimationOptions() {
        animationSettingsImageView.setOnClickListener { animationOptionsManager.showAnimationOptionsDialog(this) }
        animationOptionsManager.onOptionsChanged = { updateMapMarkersVisibility() }
    }

    private fun setupLocationMarkerAnimations() {
        enhancedLocationAnimator.positionsFlow
            .onEach { showMarkerOnMap(it, isRaw = false) }
            .launchIn(scope)

        enhancedLocationAnimator.cameraPositionsFlow
            .onEach { moveCamera(it) }
            .launchIn(scope)

        rawLocationAnimator.positionsFlow
            .onEach { showMarkerOnMap(it, isRaw = true) }
            .launchIn(scope)
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
        (mapFragmentContainerView.getFragment() as SupportMapFragment).let {
            it.getMapAsync { map ->
                map.uiSettings.isZoomControlsEnabled = true
                googleMap = map
            }
        }
    }

    private fun startSubscribing() {
        trackableIdEditText.text.toString().trim().let { trackableId ->
            if (trackableId.isNotEmpty()) {
                hideKeyboard(trackableIdEditText)
                showLoading()
                scope.launch {
                    try {
                        createAndStartAssetSubscriber(trackableId)
                        showStartedSubscriberLayout()
                        hideLoading()
                    } catch (exception: Exception) {
                        showToast(R.string.error_starting_subscriber_failed)
                        hideLoading()
                    }
                }
            } else {
                showToast(R.string.error_no_trackable_id)
            }
        }
    }

    @OptIn(Experimental::class)
    private suspend fun createAndStartAssetSubscriber(trackableId: String) {
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
                    .onEach { enhancedLocationAnimator.animateLocationUpdate(it, locationUpdateIntervalInMilliseconds) }
                    .launchIn(scope)
                rawLocations
                    .onEach { rawLocationAnimator.animateLocationUpdate(it, locationUpdateIntervalInMilliseconds) }
                    .launchIn(scope)
                trackableStates
                    .onEach { updateAssetState(it) }
                    .launchIn(scope)
                publisherPresence
                    .onEach { updatePresenceState(it) }
                    .launchIn(scope)
                resolutions
                    .onEach {
                        updatePublisherResolutionInfo(it)
                    }
                    .launchIn(scope)
                nextLocationUpdateIntervals
                    .onEach { locationUpdateIntervalInMilliseconds = it }
                    .launchIn(scope)
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
        subscriber?.let {
            try {
                it.resolutionPreference(newResolution)
                resolution = newResolution
                updateResolutionInfo(newResolution)
            } catch (exception: Exception) {
                showToast(R.string.error_changing_resolution_failed)
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
        resolutionAccuracyTextView.text = resolution.accuracy.name.lowercase().replaceFirstChar { it.uppercase() }
        resolutionDisplacementTextView.text =
            getString(R.string.resolution_minimum_displacement_value, resolution.minimumDisplacement)
        resolutionIntervalTextView.text =
            getString(R.string.resolution_desired_interval_value, resolution.desiredInterval)
    }

    private fun updatePublisherResolutionInfo(resolution: Resolution) {
        publisherResolutionAccuracyTextView.text =
            resolution.accuracy.name.lowercase().replaceFirstChar { it.uppercase() }
        publisherResolutionDisplacementTextView.text =
            getString(R.string.resolution_minimum_displacement_value, resolution.minimumDisplacement)
        publisherResolutionIntervalTextView.text =
            getString(R.string.resolution_desired_interval_value, resolution.desiredInterval)
    }

    private fun clearResolutionsInfo() {
        resolutionAccuracyTextView.text = ""
        resolutionDisplacementTextView.text = ""
        resolutionIntervalTextView.text = ""
        publisherResolutionAccuracyTextView.text = ""
        publisherResolutionDisplacementTextView.text = ""
        publisherResolutionIntervalTextView.text = ""
    }

    private fun updateAssetState(trackableState: TrackableState) {
        val textId = when (trackableState) {
            is TrackableState.Online -> R.string.asset_status_online
            is TrackableState.Publishing -> R.string.asset_status_online
            is TrackableState.Offline -> R.string.asset_status_offline
            is TrackableState.Failed -> R.string.asset_status_failed
        }
        val textColorId = when (trackableState) {
            is TrackableState.Online -> R.color.black
            is TrackableState.Publishing -> R.color.black
            is TrackableState.Offline -> R.color.mid_grey
            is TrackableState.Failed -> R.color.black
        }
        val backgroundColorId = when (trackableState) {
            is TrackableState.Online -> R.color.asset_online
            is TrackableState.Publishing -> R.color.asset_online
            is TrackableState.Offline -> R.color.asset_offline
            is TrackableState.Failed -> R.color.asset_failed
        }
        assetStateTextView.text = getString(textId)
        assetStateTextView.setTextColor(ContextCompat.getColor(this, textColorId))
        assetStateTextView.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, backgroundColorId))
    }

    private fun updatePresenceState(trackableState: Boolean) {
        val textColorId = when (trackableState) {
            true -> R.color.black
            false -> R.color.mid_grey
        }

        val backgroundColorId = when (trackableState) {
            true -> R.color.asset_online
            false -> R.color.asset_offline
        }

        assetPresenceStateTextView.text = if (trackableState) "Presence: Online" else "Presence: Offline"
        assetPresenceStateTextView.setTextColor(ContextCompat.getColor(this, textColorId))
        assetPresenceStateTextView.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, backgroundColorId))
    }

    private fun stopSubscribing() {
        showLoading()
        scope.launch {
            try {
                subscriber?.stop()
                subscriber = null
                enhancedMarker = null
                enhancedAccuracyCircle = null
                rawMarker = null
                rawAccuracyCircle = null
                showStoppedSubscriberLayout()
                hideLoading()
            } catch (exception: Exception) {
                hideLoading()
                showToast(R.string.error_stopping_subscriber_failed)
            }
        }
    }

    private fun showMarkerOnMap(newPosition: Position, isRaw: Boolean) {
        val currentMarker = if (isRaw) rawMarker else enhancedMarker
        val currentAccuracyCircle = if (isRaw) rawAccuracyCircle else enhancedAccuracyCircle
        if (currentMarker == null || currentAccuracyCircle == null) {
            createMarker(newPosition, isRaw)
            if (!isRaw) {
                moveCamera(newPosition, ZOOM_LEVEL_STREETS)
            }
        } else {
            updateMarker(newPosition, currentMarker, currentAccuracyCircle, isRaw)
        }
    }

    private fun createMarker(newPosition: Position, isRaw: Boolean) {
        googleMap?.apply {
            val position = LatLng(newPosition.latitude, newPosition.longitude)
            val marker = addMarker(
                MarkerOptions()
                    .position(position)
                    .icon(getMarkerIcon(newPosition.bearing, isRaw))
                    .alpha(if (isRaw) 0.5f else 1f)
            )
            val accuracyCircle = addCircle(
                CircleOptions()
                    .center(position)
                    .radius(newPosition.accuracy.toDouble())
                    .strokeColor(if (isRaw) COLOR_RED else COLOR_ORANGE)
                    .fillColor(if (isRaw) COLOR_RED_TRANSPARENT else COLOR_ORANGE_TRANSPARENT)
                    .strokeWidth(2f)
            )
            if (isRaw) {
                rawMarker = marker
                rawAccuracyCircle = accuracyCircle
            } else {
                enhancedMarker = marker
                enhancedAccuracyCircle = accuracyCircle
            }
        }
    }

    private fun updateMarker(newPosition: Position, marker: Marker, accuracyCircle: Circle, isRaw: Boolean) {
        val position = LatLng(newPosition.latitude, newPosition.longitude)
        marker.setIcon(getMarkerIcon(newPosition.bearing, isRaw))
        marker.position = position
        accuracyCircle.center = position
        accuracyCircle.radius = newPosition.accuracy.toDouble()
    }

    private fun moveCamera(newPosition: Position, zoomLevel: Float? = null) {
        val position = LatLng(newPosition.latitude, newPosition.longitude)
        val cameraPosition = if (zoomLevel == null) {
            CameraUpdateFactory.newLatLng(position)
        } else {
            CameraUpdateFactory.newLatLngZoom(position, zoomLevel)
        }
        if (animationOptionsManager.animateCameraMovement && zoomLevel == null) {
            googleMap?.animateCamera(cameraPosition)
        } else {
            googleMap?.moveCamera(cameraPosition)
        }
    }

    private fun getMarkerIcon(bearing: Float, isRaw: Boolean) =
        if (isRaw) BitmapDescriptorFactory.fromResource(getMarkerResourceIdByBearing(bearing, true))
        else BitmapDescriptorFactory.fromResource(getMarkerResourceIdByBearing(bearing, false))

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

    private fun showToast(@StringRes stringResourceId: Int) {
        Toast.makeText(this, stringResourceId, Toast.LENGTH_SHORT).show()
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

    private fun showStartedSubscriberLayout() {
        googleMap?.clear()
        googleMap?.setOnCameraIdleListener { updateResolutionBasedOnZoomLevel() }
        showAssetInformation()
        trackableIdEditText.isEnabled = false
        updateResolutionInfo(resolution)
        changeStartButtonText(true)
    }

    private fun showStoppedSubscriberLayout() {
        googleMap?.setOnCameraIdleListener { }
        hideAssetInformation()
        trackableIdEditText.isEnabled = true
        clearResolutionsInfo()
        changeStartButtonText(false)
    }

    private fun showLoading() {
        progressIndicator.visibility = View.VISIBLE
        startButton.isEnabled = false
        startButton.hideText()
    }

    private fun hideLoading() {
        progressIndicator.visibility = View.GONE
        startButton.isEnabled = true
        startButton.showText()
    }

    private fun updateMapMarkersVisibility() {
        rawMarker?.isVisible = animationOptionsManager.showRawMarker
        rawAccuracyCircle?.isVisible = animationOptionsManager.showRawAccuracy
        enhancedMarker?.isVisible = animationOptionsManager.showEnhancedMarker
        enhancedAccuracyCircle?.isVisible = animationOptionsManager.showEnhancedAccuracy
    }
}
