package com.ably.tracking.example.publisher

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.ably.tracking.Location
import com.ably.tracking.TrackableState
import com.ably.tracking.publisher.Trackable
import kotlinx.android.synthetic.main.activity_trackable_details.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

const val TRACKABLE_ID_EXTRA = "TRACKABLE_ID"

class TrackableDetailsActivity : PublisherServiceActivity() {
    private lateinit var appPreferences: AppPreferences
    private lateinit var trackableId: String
    private var trackables: Set<Trackable>? = null

    // SupervisorJob() is used to keep the scope working after any of its children fail
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trackable_details)
        trackableId = intent.extras?.getString(TRACKABLE_ID_EXTRA)
            ?: throw Exception("Cannot open details activity without a Trackable ID")
        appPreferences = AppPreferences.getInstance(this)
        updateLocationSourceMethodInfo()

        trackableIdTextView.text = trackableId
        listenForPublisherChanges(publisherService)

        stopTrackingButton.setOnClickListener {
            stopTracking()
        }
        showMapButton.setOnClickListener {
            showMapScreen()
        }
    }

    override fun onPublisherServiceConnected(publisherService: PublisherService) {
        listenForPublisherChanges(publisherService)
    }

    private fun listenForPublisherChanges(publisherService: PublisherService?) {
        publisherService?.publisher?.apply {
            getTrackableState(trackableId)
                ?.onEach { updateAssetStateInfo(it) }
                ?.launchIn(scope)
            locations
                .onEach { updateLocationInfo(it.location) }
                .launchIn(scope)
            trackables
                .onEach { this@TrackableDetailsActivity.trackables = it }
                .launchIn(scope)
        }
    }

    private fun updateLocationSourceMethodInfo() {
        locationSourceMethodTextView.text = appPreferences.getLocationSource().displayName
    }

    private fun stopTracking() {
        AlertDialog.Builder(this)
            .setTitle(R.string.stop_tracking_dialog_title)
            .setMessage(R.string.stop_tracking_dialog_message)
            .setPositiveButton(R.string.dialog_positive_button) { _, _ ->
                scope.launch {
                    val trackableToRemove = trackables?.find { trackable -> trackable.id == trackableId }
                    trackableToRemove?.let { publisherService?.publisher?.remove(it) }
                    finish()
                }
            }
            .setNegativeButton(R.string.dialog_negative_button, null)
            .show()
    }

    private fun updateLocationInfo(location: Location) {
        val lat = location.latitude.toString()
        val lon = location.longitude.toString()
        val bearing = location.bearing.toString()
        latitudeValueTextView.text = if (lat.length > 7) lat.substring(0, 7) else lat
        longitudeValueTextView.text = if (lon.length > 7) lon.substring(0, 7) else lon
        bearingValueTextView.text = if (bearing.length > 7) bearing.substring(0, 7) else bearing
    }

    private fun updateAssetStateInfo(state: TrackableState) {
        val textId: Int
        val textColor: Int
        val colorId: Int
        when (state) {
            is TrackableState.Online -> {
                textId = R.string.online
                colorId = R.color.asset_status_online
                textColor = R.color.black
            }
            is TrackableState.Offline -> {
                textId = R.string.offline
                colorId = R.color.asset_status_offline
                textColor = R.color.mid_grey
            }
            is TrackableState.Failed -> {
                textId = R.string.failed
                colorId = R.color.asset_status_failed
                textColor = R.color.black
            }
        }
        assetStateValueTextView.text = getString(textId)
        assetStateValueTextView.setTextColor(ContextCompat.getColor(this, textColor))
        assetStateValueTextView.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, colorId))
    }

    private fun showMapScreen() {
        startActivity(Intent(this, MapActivity::class.java))
    }
}
