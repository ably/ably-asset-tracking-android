package com.ably.tracking.example.publisher

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import kotlinx.android.synthetic.main.activity_map.mapView
import kotlinx.android.synthetic.main.activity_set_destination.acceptDestinationButton

class SetDestinationActivity : AppCompatActivity() {
    companion object {
        val EXTRA_LATITUDE = "extra_latitude"
        val EXTRA_LONGITUDE = "extra_longitude"
    }

    private var map: MapboxMap? = null
    lateinit var pointAnnotationManager: PointAnnotationManager
    private var marker: PointAnnotation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_destination)
        setupMap()
        setupDestinationClickListener()
        pointAnnotationManager = mapView.annotations.createPointAnnotationManager()
    }

    private fun setupMap() {
        mapView.getMapboxMap().let { mapboxMap ->
            mapboxMap.loadStyleUri(Style.MAPBOX_STREETS)
            mapboxMap.addOnMapClickListener {
                showDestinationMarker(it)
                enableAcceptDestinationButton()
                true
            }
            map = mapboxMap
        }
    }

    private fun setupDestinationClickListener() {
        acceptDestinationButton.setOnClickListener {
            marker?.point?.let { destinationPoint ->
                setResult(
                    RESULT_OK,
                    Intent().apply {
                        putExtra(EXTRA_LATITUDE, destinationPoint.latitude())
                        putExtra(EXTRA_LONGITUDE, destinationPoint.longitude())
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

    private fun showDestinationMarker(location: Point) {
        if (marker == null) {
            createBitmapFromVectorDrawable(R.drawable.ic_red_map_marker)?.let { iconBitmap ->
                marker = pointAnnotationManager.create(
                    PointAnnotationOptions()
                        .withPoint(location)
                        .withIconImage(iconBitmap)
                )
            }
        } else {
            marker!!.point = location
            pointAnnotationManager.update(marker!!)
        }
    }
}
