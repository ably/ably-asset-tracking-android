package com.ably.tracking.example.publisher

import android.Manifest
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution
import com.ably.tracking.publisher.DefaultProximity
import com.ably.tracking.publisher.DefaultResolutionConstraints
import com.ably.tracking.publisher.DefaultResolutionSet
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

class AddTrackableActivity : PublisherServiceActivity() {
    private lateinit var appPreferences: AppPreferences

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
    }

    private fun setupResolutionFields() {
        accuracySpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, Accuracy.values()).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        accuracySpinner.setSelection(appPreferences.getResolutionAccuracy().ordinal)
        desiredIntervalEditText.setText(appPreferences.getResolutionDesiredInterval().toString())
        minimumDisplacementEditText.setText(
            appPreferences.getResolutionMinimumDisplacement().toString()
        )
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
    override fun onPublisherServiceConnected(publisherService: PublisherService) {
        // If the publisher is not started it means that we've just created the service and we should add a trackable
        if (!publisherService.isPublisherStarted) {
            startPublisherAndAddTrackable(getTrackableId())
        }
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    private fun addTrackableClicked() {
        getTrackableId().let { trackableId ->
            if (trackableId.isNotEmpty()) {
                showLoading()
                if (isPublisherServiceStarted()) {
                    startPublisherAndAddTrackable(trackableId)
                } else {
                    startAndBindPublisherService()
                }
            } else {
                showLongToast("Insert tracking ID")
            }
        }
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    private fun startPublisherAndAddTrackable(trackableId: String) {
        publisherService.let { publisherService ->
            if (publisherService == null) {
                onAddTrackableFailed()
                return
            }
            scope.launch(
                CoroutineExceptionHandler { _, _ -> onAddTrackableFailed() }
            ) {
                if (!publisherService.isPublisherStarted) {
                    val locationHistoryData = when (appPreferences.getLocationSource()) {
                        LocationSourceType.S3_FILE -> downloadLocationHistoryData()
                        else -> null
                    }
                    publisherService.startPublisher(createLocationSource(locationHistoryData))
                }
                publisherService.publisher!!.track(createTrackable(trackableId))
                showTrackableDetailsScreen(trackableId)
                finish()
            }
        }
    }

    private fun onAddTrackableFailed() {
        showLongToast("Error when adding the trackable")
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
                    showLongToast("S3 not initialized - cannot download history data")
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
        addTrackableButton.textScaleX = 0f
    }

    private fun hideLoading() {
        progressIndicator.visibility = View.GONE
        addTrackableButton.isEnabled = true
        trackableIdEditText.isEnabled = true
        accuracySpinner.isEnabled = true
        desiredIntervalEditText.isEnabled = true
        minimumDisplacementEditText.isEnabled = true
        addTrackableButton.textScaleX = 1f
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
}
