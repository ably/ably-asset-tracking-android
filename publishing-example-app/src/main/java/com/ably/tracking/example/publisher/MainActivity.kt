package com.ably.tracking.example.publisher

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_main.addTrackableFab
import kotlinx.android.synthetic.main.activity_main.emptyStateContainer
import kotlinx.android.synthetic.main.activity_main.locationSourceMethodTextView
import kotlinx.android.synthetic.main.activity_main.publisherServiceSwitch
import kotlinx.android.synthetic.main.activity_main.settingsImageView
import kotlinx.android.synthetic.main.activity_main.trackablesRecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

class MainActivity : PublisherServiceActivity() {
    private lateinit var appPreferences: AppPreferences
    private val trackablesAdapter = TrackablesAdapter()
    private var trackablesUpdateJob: Job? = null

    // SupervisorJob() is used to keep the scope working after any of its children fail
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("Hello via Timber")
        setContentView(R.layout.activity_main)
        appPreferences = AppPreferences.getInstance(this)
        updateLocationSourceMethodInfo()

        PermissionsHelper.requestLocationPermission(this)

        settingsImageView.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        addTrackableFab.setOnClickListener { onAddTrackableClick() }
        publisherServiceSwitch.setOnClickListener { onServiceSwitchClick(publisherServiceSwitch.isChecked) }

        trackablesRecyclerView.adapter = trackablesAdapter
        trackablesRecyclerView.layoutManager = LinearLayoutManager(this)
        trackablesRecyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        trackablesAdapter.onItemClickedCallback = { onTrackableClicked(it.id) }
    }

    override fun onPublisherServiceConnected(publisherService: PublisherService) {
        indicatePublisherServiceIsOn()
        publisherService.publisher?.let { publisher ->
            trackablesUpdateJob = publisher.trackables
                .onEach {
                    trackablesAdapter.trackables = it.toList()
                    if (it.isEmpty() && publisherService.publisher != null) {
                        hideTrackablesList()
                        try {
                            publisherService.publisher?.stop()
                            publisherService.publisher = null
                        } catch (e: Exception) {
                            showLongToast(R.string.error_stop_publisher)
                        }
                    } else {
                        showTrackablesList()
                    }
                }
                .launchIn(scope)
        }
    }

    override fun onPublisherServiceDisconnected() {
        indicatePublisherServiceIsOff()
        trackablesUpdateJob?.cancel()
    }

    override fun onStart() {
        super.onStart()
        updateLocationSourceMethodInfo()
    }

    private fun onTrackableClicked(trackableId: String) {
        startActivity(
            Intent(this, TrackableDetailsActivity::class.java).apply {
                putExtra(TRACKABLE_ID_EXTRA, trackableId)
            }
        )
    }

    private fun onServiceSwitchClick(isSwitchingOn: Boolean) {
        if (isSwitchingOn) {
            startAndBindPublisherService()
        } else {
            if (trackablesAdapter.trackables.isEmpty()) {
                stopPublisherService()
            } else {
                showCannotStopServiceDialog()
                indicatePublisherServiceIsOn()
            }
        }
    }

    private fun onAddTrackableClick() {
        if (isPublisherServiceStarted()) {
            showAddTrackableScreen()
        } else {
            showServiceNotStartedDialog()
        }
    }

    private fun updateLocationSourceMethodInfo() {
        locationSourceMethodTextView.text = getString(appPreferences.getLocationSource().displayNameResourceId)
    }

    private fun showAddTrackableScreen() {
        if (PermissionsHelper.hasFineOrCoarseLocationPermissionGranted(this)) {
            startActivity(Intent(this, AddTrackableActivity::class.java))
        } else {
            PermissionsHelper.requestLocationPermission(this)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionsHelper.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults,
            onLocationPermissionGranted = { showLongToast(R.string.info_permission_granted) }
        )
    }

    private fun showTrackablesList() {
        trackablesRecyclerView.visibility = View.VISIBLE
        emptyStateContainer.visibility = View.GONE
    }

    private fun hideTrackablesList() {
        trackablesRecyclerView.visibility = View.GONE
        emptyStateContainer.visibility = View.VISIBLE
    }

    private fun indicatePublisherServiceIsOn() {
        publisherServiceSwitch.isChecked = true
    }

    private fun indicatePublisherServiceIsOff() {
        publisherServiceSwitch.isChecked = false
    }

    private fun showServiceNotStartedDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.service_not_started_dialog_title)
            .setMessage(R.string.service_not_started_dialog_message)
            .setPositiveButton(R.string.dialog_positive_button, null)
            .show()
    }

    private fun showCannotStopServiceDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.cannot_stop_service_dialog_title)
            .setMessage(R.string.cannot_stop_service_dialog_message)
            .setPositiveButton(R.string.dialog_positive_button, null)
            .show()
    }
}
