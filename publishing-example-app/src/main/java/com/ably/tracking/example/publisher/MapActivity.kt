// Mapbox has deprecated the Marker class and recommends using the Annotation Plugin
// but to keep things as simple as possible we're going to still use the Marker approach
@file:Suppress("DEPRECATION")

package com.ably.tracking.example.publisher

import android.location.Location
import android.os.Bundle
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import kotlinx.android.synthetic.main.activity_map.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MapActivity : PublisherServiceActivity() {
    // SupervisorJob() is used to keep the scope working after any of its children fail
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var map: MapboxMap? = null
    private var marker: Marker? = null
    private val INITIAL_ZOOM_LEVEL = 15.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, BuildConfig.MAPBOX_ACCESS_TOKEN)
        setContentView(R.layout.activity_map)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { mapboxMap ->
            mapboxMap.setStyle(Style.MAPBOX_STREETS) {
                map = mapboxMap
            }
        }
    }

    override fun onPublisherServiceConnected(publisherService: PublisherService) {
        publisherService.publisher?.apply {
            locations
                .onEach { updateMap(it.location.toAndroid()) }
                .launchIn(scope)
        }
    }

    private fun updateMap(location: Location) {
        map?.apply {
            val newPosition = LatLng(location)
            if (marker == null) {
                marker = addMarker(MarkerOptions().position(newPosition))
                moveCamera(CameraUpdateFactory.newLatLngZoom(newPosition, INITIAL_ZOOM_LEVEL))
            } else {
                marker?.position = newPosition
                moveCamera(CameraUpdateFactory.newLatLng(newPosition))
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }
}
