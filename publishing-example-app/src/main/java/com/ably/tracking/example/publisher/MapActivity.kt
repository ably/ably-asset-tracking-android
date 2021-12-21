package com.ably.tracking.example.publisher

import android.location.Location
import android.os.Bundle
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import kotlinx.android.synthetic.main.activity_map.mapView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MapActivity : PublisherServiceActivity() {
    // SupervisorJob() is used to keep the scope working after any of its children fail
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var map: MapboxMap? = null
    lateinit var pointAnnotationManager: PointAnnotationManager
    private var annotation: PointAnnotation? = null
    private val INITIAL_ZOOM_LEVEL = 15.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        mapView.getMapboxMap().let { mapboxMap ->
            mapboxMap.loadStyleUri(Style.MAPBOX_STREETS)
            map = mapboxMap
        }
        pointAnnotationManager = mapView.annotations.createPointAnnotationManager(mapView)
    }

    override fun onPublisherServiceConnected(publisherService: PublisherService) {
        publisherService.publisher?.apply {
            locations
                .onEach { updateMap(it.location.toAndroid()) }
                .launchIn(scope)
        }
    }

    private fun updateMap(location: Location) {
        val newPosition = Point.fromLngLat(location.longitude, location.latitude)
        if (annotation == null) {
            createBitmapFromVectorDrawable(R.drawable.ic_red_map_marker)?.let { iconBitmap ->
                annotation = pointAnnotationManager.create(
                    PointAnnotationOptions()
                        .withPoint(newPosition)
                        .withIconImage(iconBitmap)
                )
                map?.setCamera(CameraOptions.Builder().center(newPosition).zoom(INITIAL_ZOOM_LEVEL).build())
            }
        } else {
            annotation!!.point = newPosition
            pointAnnotationManager.update(annotation!!)
            map?.setCamera(CameraOptions.Builder().center(newPosition).build())
        }
    }
}
