package com.ably.tracking.example.publisher

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.ColorStateList
import android.location.Location
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.ably.tracking.ConnectionState
import kotlinx.android.synthetic.main.activity_main.ablyConnectionStatusImageView
import kotlinx.android.synthetic.main.activity_main.ablyConnectionStatusValueTextView
import kotlinx.android.synthetic.main.activity_main.bearingValueTextView
import kotlinx.android.synthetic.main.activity_main.latitudeValueTextView
import kotlinx.android.synthetic.main.activity_main.locationSourceMethodTextView
import kotlinx.android.synthetic.main.activity_main.longitudeValueTextView
import kotlinx.android.synthetic.main.activity_trackable_details.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private const val NO_FLAGS = 0

class TrackableDetailsActivity : AppCompatActivity() {
    private var publisherService: PublisherService? = null
    private lateinit var appPreferences: AppPreferences

    // SupervisorJob() is used to keep the scope working after any of its children fail
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val publisherServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, serviceBinder: IBinder) {
            (serviceBinder as PublisherService.Binder).getService().let { service ->
                publisherService = service
                listenForPublisherChanges(service)
            }
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            publisherService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trackable_details)
        appPreferences = AppPreferences(this) // TODO - Add some DI (Koin)?
        updateLocationSourceMethodInfo()

        listenForPublisherChanges(publisherService)

        bindService(createServiceIntent(), publisherServiceConnection, NO_FLAGS)
        stopTrackingButton.setOnClickListener {
            stopTracking()
        }
    }

    private fun listenForPublisherChanges(publisherService: PublisherService?) {
        publisherService?.publisher?.apply {
            connectionStates
                .onEach { updateAblyStateInfo(it.state) }
                .launchIn(scope)
            locations
                .onEach { updateLocationInfo(it.location) }
                .launchIn(scope)
        }
    }

    private fun createServiceIntent() = Intent(this, PublisherService::class.java)

    private fun updateLocationSourceMethodInfo() {
        locationSourceMethodTextView.text = appPreferences.getLocationSource()
    }

    private fun stopTracking() {
        AlertDialog.Builder(this)
            .setTitle(R.string.stop_tracking_dialog_title)
            .setMessage(R.string.stop_tracking_dialog_message)
            .setPositiveButton(R.string.dialog_positive_button) { _, _ ->
                scope.launch {
//                     TODO - remove trackable from publisher and finish the activity
//                    publisher?.remove()
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
