package com.ably.tracking.example.publisher

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution
import com.ably.tracking.publisher.DefaultProximity
import com.ably.tracking.publisher.DefaultResolutionConstraints
import com.ably.tracking.publisher.DefaultResolutionSet
import com.ably.tracking.publisher.Destination
import com.ably.tracking.publisher.LocationHistoryData
import com.ably.tracking.publisher.LocationSource
import com.ably.tracking.publisher.LocationSourceAbly
import com.ably.tracking.publisher.LocationSourceRaw
import com.ably.tracking.publisher.Trackable
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.android.synthetic.main.activity_add_trackable.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val SET_DESTINATION_REQUEST_CODE = 1000

class AddTrackableActivity : PublisherServiceActivity() {
    private lateinit var appPreferences: AppPreferences
    private var destination: Destination? = null

    // SupervisorJob() is used to keep the scope working after any of its children fail
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_trackable)
        appPreferences = AppPreferences.getInstance(this)

        setTrackableIdEditTextListener()
        setupResolutionFields()
        addTrackableButton.setOnClickListener { addTrackableClicked() }
        setupTrackableInputAction()
        setDestinationButton.setOnClickListener {
            var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK && result.data != null) {
                    val data: Intent = result.data!!
                    if (
                        data.hasExtra(SetDestinationActivity.EXTRA_LATITUDE) &&
                        data.hasExtra(SetDestinationActivity.EXTRA_LONGITUDE)
                    ) {
                        val latitude = data.getDoubleExtra(SetDestinationActivity.EXTRA_LATITUDE, 0.0)
                        val longitude = data.getDoubleExtra(SetDestinationActivity.EXTRA_LONGITUDE, 0.0)
                        updateDestination(latitude, longitude)
                    }
                }
            }
            resultLauncher.launch(Intent(this, SetDestinationActivity::class.java))
        }
    }

    private fun updateDestination(latitude: Double, longitude: Double) {
        latitudeValueTextView.text = latitude.toString().take(7)
        longitudeValueTextView.text = longitude.toString().take(7)
        destination = Destination(latitude, longitude)
    }

    private fun setupResolutionFields() {
        accuracySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, Accuracy.values()).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        accuracySpinner.setSelection(appPreferences.getResolutionAccuracy().ordinal)
        desiredIntervalEditText.setText(appPreferences.getResolutionDesiredInterval().toString())
        minimumDisplacementEditText.setText(appPreferences.getResolutionMinimumDisplacement().toString())
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    private fun setupTrackableInputAction() {
        trackableIdEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard(trackableIdEditText)
                addTrackableClicked()
                true
            } else {
                false
            }
        }
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    private fun addTrackableClicked() {
        getTrackableId().let { trackableId ->
            if (!PermissionsHelper.hasFineOrCoarseLocationPermissionGranted(this)) {
                PermissionsHelper.requestLocationPermission(this)
                return
            }
            if (trackableId.isNotEmpty()) {
                showLoading()
                if (isPublisherServiceStarted()) {
                    publisherService?.let { publisherService ->
                        scope.launch(CoroutineExceptionHandler { _, _ -> onAddTrackableFailed() }) {
                            if (!publisherService.isPublisherStarted) {
                                startPublisher(publisherService)
                            }
                            publisherService.publisher!!.track(createTrackable(trackableId))
                            showTrackableDetailsScreen(trackableId)
                            finish()
                        }
                    }
                } else {
                    onAddTrackableFailed(R.string.error_publisher_service_not_started)
                }
            } else {
                showLongToast(R.string.error_no_trackable_id)
            }
        }
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    private suspend fun startPublisher(publisherService: PublisherService) {
        val locationHistoryData = when (appPreferences.getLocationSource()) {
            LocationSourceType.S3_FILE -> downloadLocationHistoryData()
            else -> null
        }
        publisherService.startPublisher(createLocationSource(locationHistoryData))
    }

    private fun onAddTrackableFailed(@StringRes messageResourceId: Int = R.string.error_trackable_adding_failed) {
        showLongToast(messageResourceId)
        hideLoading()
    }

    private fun createResolution(): Resolution {
        return Resolution(
            accuracySpinner.selectedItem as Accuracy,
            desiredIntervalEditText.text.toString().toLong(),
            minimumDisplacementEditText.text.toString().toDouble()
        )
    }

    private fun createTrackable(trackableId: String): Trackable =
        Trackable(
            trackableId,
            destination = destination,
            constraints = DefaultResolutionConstraints(
                DefaultResolutionSet(createResolution()),
                DefaultProximity(spatial = 1.0),
                batteryLevelThreshold = 10.0f,
                lowBatteryMultiplier = 2.0f
            )
        )

    private fun showTrackableDetailsScreen(trackableId: String) {
        startActivity(
            Intent(this@AddTrackableActivity, TrackableDetailsActivity::class.java).apply {
                putExtra(TRACKABLE_ID_EXTRA, trackableId)
            }
        )
    }

    private fun createLocationSource(historyData: LocationHistoryData? = null): LocationSource? =
        when (appPreferences.getLocationSource()) {
            LocationSourceType.PHONE -> null
            LocationSourceType.ABLY_CHANNEL -> LocationSourceAbly.create(appPreferences.getSimulationChannel())
            LocationSourceType.S3_FILE -> {
                if (historyData != null)
                    LocationSourceRaw.create(historyData)
                else
                    throw Exception("Location history data cannot be null")
            }
        }

    private suspend fun downloadLocationHistoryData(): LocationHistoryData {
        return suspendCoroutine { continuation ->
            S3Helper.downloadHistoryData(
                this,
                appPreferences.getS3File(),
                onHistoryDataDownloaded = { continuation.resume(it) },
                onError = { continuation.resumeWithException(it) },
                onUninitialized = {
                    showLongToast(R.string.error_s3_not_initialized_history_data_download_failed)
                    continuation.resumeWithException(Exception("S3 not initialized"))
                }
            )
        }
    }

    private fun showLoading() {
        progressIndicator.visibility = View.VISIBLE
        addTrackableButton.isEnabled = false
        trackableIdEditText.isEnabled = false
        accuracySpinner.isEnabled = false
        desiredIntervalEditText.isEnabled = false
        minimumDisplacementEditText.isEnabled = false
        addTrackableButton.hideText()
    }

    private fun hideLoading() {
        progressIndicator.visibility = View.GONE
        addTrackableButton.isEnabled = true
        trackableIdEditText.isEnabled = true
        accuracySpinner.isEnabled = true
        desiredIntervalEditText.isEnabled = true
        minimumDisplacementEditText.isEnabled = true
        addTrackableButton.showText()
    }

    private fun getTrackableId(): String = trackableIdEditText.text.toString().trim()

    private fun setTrackableIdEditTextListener() {
        trackableIdEditText.addTextChangedListener { trackableId ->
            trackableId?.trim()?.let { changeAddButtonColor(it.isNotEmpty()) }
        }
    }

    private fun changeAddButtonColor(isActive: Boolean) {
        if (isActive) {
            addTrackableButton.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.button_active))
            addTrackableButton.setTextColor(ContextCompat.getColor(this, R.color.white))
        } else {
            addTrackableButton.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.button_inactive))
            addTrackableButton.setTextColor(ContextCompat.getColor(this, R.color.mid_grey))
        }
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionsHelper.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults,
            onLocationPermissionGranted = {
                addTrackableClicked()
            }
        )
    }
}
