package com.ably.tracking.example.publisher

import android.content.res.ColorStateList
import android.location.Location
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.ably.tracking.ConnectionState
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
        appPreferences = AppPreferences(this) // TODO - Add some DI (Koin)?
        updateLocationSourceMethodInfo()

        trackableIdTextView.text = trackableId
        listenForPublisherChanges(publisherService)

        stopTrackingButton.setOnClickListener {
            stopTracking()
        }
    }

    override fun onPublisherServiceConnected(publisherService: PublisherService) {
        listenForPublisherChanges(publisherService)
    }

    private fun listenForPublisherChanges(publisherService: PublisherService?) {
        publisherService?.publisher?.apply {
            connectionStates
                .onEach { updateAblyStateInfo(it.state) }
                .launchIn(scope)
            locations
                .onEach { updateLocationInfo(it.location) }
                .launchIn(scope)
            trackables
                .onEach { this@TrackableDetailsActivity.trackables = it }
                .launchIn(scope)
        }
    }

    private fun updateLocationSourceMethodInfo() {
        locationSourceMethodTextView.text = appPreferences.getLocationSource()
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

    private fun updateAblyStateInfo(state: ConnectionState) {
        val isAblyConnected = state == ConnectionState.CONNECTED
        changeAblyStatusInfo(isAblyConnected)
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
}
