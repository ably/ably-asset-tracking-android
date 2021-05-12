package com.ably.tracking.example.publisher

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions

private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
private const val REQUEST_LOCATION_PERMISSION = 1

class MainActivity : PublisherServiceActivity() {
    private lateinit var appPreferences: AppPreferences
    private val trackablesAdapter = TrackablesAdapter()
    private var trackablesUpdateJob: Job? = null

    // SupervisorJob() is used to keep the scope working after any of its children fail
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        appPreferences = AppPreferences(this)
        updateLocationSourceMethodInfo()

        requestLocationPermission()

        settingsFab.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        addTrackableFab.setOnClickListener {
            showAddTrackableScreen()
        }

        trackablesRecyclerView.adapter = trackablesAdapter
        trackablesRecyclerView.layoutManager = LinearLayoutManager(this)
        trackablesRecyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        trackablesAdapter.onItemClickedCallback = { onTrackableClicked(it.id) }
    }

    override fun onPublisherServiceConnected(publisherService: PublisherService) {
        publisherService.publisher?.let { publisher ->
            trackablesUpdateJob = publisher.trackables
                .onEach {
                    trackablesAdapter.trackables = it.toList()
                    if (it.isEmpty() && publisherService.publisher != null) {
                        try {
                            publisherService.publisher?.stop()
                            publisherService.publisher = null
                        } catch (e: Exception) {
                            // TODO check Result (it) for failure and report accordingly
                        }
                        stopPublisherService()
                    }
                }
                .launchIn(scope)
        }
    }

    override fun onPublisherServiceDisconnected() {
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

    private fun updateLocationSourceMethodInfo() {
        locationSourceMethodTextView.text = appPreferences.getLocationSource()
    }

    private fun requestLocationPermission() {
        if (!EasyPermissions.hasPermissions(this, *REQUIRED_PERMISSIONS)) {
            EasyPermissions.requestPermissions(
                this,
                "Please grant the location permission",
                REQUEST_LOCATION_PERMISSION,
                *REQUIRED_PERMISSIONS
            )
        }
    }

    @AfterPermissionGranted(REQUEST_LOCATION_PERMISSION)
    fun onLocationPermissionGranted() {
        showToast("Permission granted")
    }

    private fun showAddTrackableScreen() {
        if (hasFineOrCoarseLocationPermissionGranted(this)) {
            startActivity(Intent(this, AddTrackableActivity::class.java))
        } else {
            requestLocationPermission()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
