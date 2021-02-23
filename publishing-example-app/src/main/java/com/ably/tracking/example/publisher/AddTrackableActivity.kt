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

        addTrackableButton.setOnClickListener { addTrackable() }
    }

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION])
    private fun addTrackable() {
        trackableIdEditText.text.toString().trim().let { trackableId ->
            if (trackableId.isNotEmpty()) {
                addTrackableButton.isEnabled = false
                progressIndicator.visibility = View.VISIBLE
                if (getLocationSourceType() == MainActivity.LocationSourceType.S3) {
                    downloadLocationHistoryData { startPublisherAndAddTrackable(trackableId, it) }
                } else {
                    startPublisherAndAddTrackable(trackableId)
                }
            } else {
                showToast("Insert tracking ID")
            }
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
                    startActivity(Intent(this@AddTrackableActivity, TrackableDetailsActivity::class.java))
                    finish()
                } catch (exception: Exception) {
                    showToast("Error when adding the trackable")
                    progressIndicator.visibility = View.GONE
                    addTrackableButton.isEnabled = true
                }
            }
        }
    }

    private fun createLocationSource(historyData: String? = null): LocationSource? =
        when (getLocationSourceType()) {
            MainActivity.LocationSourceType.ABLY -> LocationSourceAbly(appPreferences.getSimulationChannel())
            MainActivity.LocationSourceType.S3 -> LocationSourceRaw(historyData!!)
            MainActivity.LocationSourceType.PHONE -> null
        }

    private fun getLocationSourceType() =
        when (appPreferences.getLocationSource()) {
            getString(R.string.location_source_ably) -> MainActivity.LocationSourceType.ABLY
            getString(R.string.location_source_s3) -> MainActivity.LocationSourceType.S3
            else -> MainActivity.LocationSourceType.PHONE
        }

    private fun downloadLocationHistoryData(onHistoryDataDownloaded: (historyData: String) -> Unit) {
        S3Helper.downloadHistoryData(
            this,
            appPreferences.getS3File(),
            onHistoryDataDownloaded = { onHistoryDataDownloaded(it) },
            onUninitialized = { showToast("S3 not initialized - cannot download history data") }
        )
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
