// Mapbox has deprecated the Marker class and recommends using the Annotation Plugin
// but to keep things as simple as possible we're going to still use the Marker approach
@file:Suppress("DEPRECATION")

package com.ably.tracking.example.publisher

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import kotlinx.android.synthetic.main.activity_map.mapView
import kotlinx.android.synthetic.main.activity_set_destination.acceptDestinationButton

class SetDestinationActivity : AppCompatActivity() {
    companion object {
        val EXTRA_LATITUDE = "extra_latitude"
        val EXTRA_LONGITUDE = "extra_longitude"
    }

    private var map: MapboxMap? = null
    private var marker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, BuildConfig.MAPBOX_ACCESS_TOKEN)
        setContentView(R.layout.activity_set_destination)
        mapView.onCreate(savedInstanceState)
        setupMap()
        setupDestinationClickListener()
    }

    private fun setupMap() {
        mapView.getMapAsync { mapboxMap ->
            mapboxMap.setStyle(Style.MAPBOX_STREETS) {
                mapboxMap.addOnMapClickListener {
                    showDestinationMarker(it)
                    enableAcceptDestinationButton()
                    true
                }
                map = mapboxMap
            }
        }
    }

    private fun setupDestinationClickListener() {
        acceptDestinationButton.setOnClickListener {
            marker?.position?.let { destinationPosition ->
                setResult(
                    RESULT_OK,
                    Intent().apply {
                        putExtra(EXTRA_LATITUDE, destinationPosition.latitude)
                        putExtra(EXTRA_LONGITUDE, destinationPosition.longitude)
                    }
                )
            }
            finish()
        }
    }

    private fun enableAcceptDestinationButton() {
        acceptDestinationButton.isEnabled = true
        acceptDestinationButton.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, R.color.button_active))
    }

    private fun showDestinationMarker(location: LatLng) {
        map?.apply {
            if (marker == null) {
                marker = addMarker(MarkerOptions().position(location))
            } else {
                marker?.position = location
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
