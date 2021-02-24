package com.ably.tracking.example.publisher

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresPermission
import com.ably.tracking.Accuracy
import com.ably.tracking.Resolution
import com.ably.tracking.publisher.DefaultProximity
import com.ably.tracking.publisher.DefaultResolutionConstraints
import com.ably.tracking.publisher.DefaultResolutionSet
import com.ably.tracking.publisher.LocationSource
import com.ably.tracking.publisher.LocationSourceAbly
import com.ably.tracking.publisher.LocationSourceRaw
import com.ably.tracking.publisher.Trackable
import kotlinx.android.synthetic.main.activity_add_trackable.*
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
        appPreferences = AppPreferences(this) // TODO - Add some DI (Koin)?

        addTrackableButton.setOnClickListener { beginAddingTrackable() }
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    override fun onPublisherServiceConnected(publisherService: PublisherService) {
        // If the publisher is null it means that we've just created the service and we should add a trackable
        if (publisherService.publisher == null) {
            addTrackable(getTrackableId())
        }
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    private fun beginAddingTrackable() {
        getTrackableId().let { trackableId ->
            if (trackableId.isNotEmpty()) {
                showLoading()
                if (publisherService == null) {
                    startAndBindPublisherService()
                } else {
                    addTrackable(trackableId)
                }
            } else {
                showToast("Insert tracking ID")
            }
        }
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    private fun addTrackable(trackableId: String) {
        if (publisherService?.publisher == null) {
            if (getLocationSourceType() == LocationSourceType.S3) {
                downloadLocationHistoryData { startPublisherAndAddTrackable(trackableId, it) }
            } else {
                startPublisherAndAddTrackable(trackableId)
            }
        } else {
            addTrackableToThePublisher(trackableId)
        }
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    private fun startPublisherAndAddTrackable(trackableId: String, historyData: String? = null) {
        if (publisherService?.publisher == null) {
            publisherService?.startPublisher(
                defaultResolution = Resolution(Accuracy.MINIMUM, 1000L, 1.0),
                locationSource = createLocationSource(historyData)
            )
        }
        addTrackableToThePublisher(trackableId)
    }

    private fun addTrackableToThePublisher(trackableId: String) {
        publisherService?.publisher?.apply {
            scope.launch {
                try {
                    track(
                        Trackable(
                            trackableId,
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
                    startActivity(Intent(this@AddTrackableActivity, TrackableDetailsActivity::class.java).apply {
                        putExtra(TRACKABLE_ID_EXTRA, trackableId)
                    })
                    finish()
                } catch (exception: Exception) {
                    showToast("Error when adding the trackable")
                    hideLoading()
                }
            }
        }
    }

    private fun createLocationSource(historyData: String? = null): LocationSource? =
        when (getLocationSourceType()) {
            LocationSourceType.ABLY -> LocationSourceAbly(appPreferences.getSimulationChannel())
            LocationSourceType.S3 -> LocationSourceRaw(historyData!!)
            LocationSourceType.PHONE -> null
        }

    private fun getLocationSourceType() =
        when (appPreferences.getLocationSource()) {
            getString(R.string.location_source_ably) -> LocationSourceType.ABLY
            getString(R.string.location_source_s3) -> LocationSourceType.S3
            else -> LocationSourceType.PHONE
        }

    private fun downloadLocationHistoryData(onHistoryDataDownloaded: (historyData: String) -> Unit) {
        S3Helper.downloadHistoryData(
            this,
            appPreferences.getS3File(),
            onHistoryDataDownloaded = { onHistoryDataDownloaded(it) },
            onUninitialized = { showToast("S3 not initialized - cannot download history data") }
        )
    }

    private fun showLoading() {
        progressIndicator.visibility = View.VISIBLE
        addTrackableButton.isEnabled = false
        trackableIdEditText.isEnabled = false
    }

    private fun hideLoading() {
        progressIndicator.visibility = View.GONE
        addTrackableButton.isEnabled = true
        trackableIdEditText.isEnabled = true
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun getTrackableId(): String = trackableIdEditText.text.toString().trim()
}
