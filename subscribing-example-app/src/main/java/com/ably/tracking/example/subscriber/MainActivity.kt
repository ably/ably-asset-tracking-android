package com.ably.tracking.example.subscriber

import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ably.tracking.Accuracy
import com.ably.tracking.ConnectionConfiguration
import com.ably.tracking.Resolution
import com.ably.tracking.subscriber.Subscriber
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_main.*

private const val CLIENT_ID = "<INSERT_CLIENT_ID_HERE>"
private const val ABLY_API_KEY = BuildConfig.ABLY_API_KEY
private const val ZOOM_LEVEL_STREETS = 15F

class MainActivity : AppCompatActivity() {
    private var subscriber: Subscriber? = null
    private var googleMap: GoogleMap? = null
    private var marker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prepareMap()

        startButton.setOnClickListener {
            if (subscriber == null) {
                startSubscribing()
            } else {
                stopSubscribing()
            }
        }
    }

    private fun prepareMap() {
        (mapFragment as SupportMapFragment).let {
            it.getMapAsync { map -> googleMap = map }
        }
    }

    private fun startSubscribing() {
        trackingIdEditText.text.toString().trim().let { trackingId ->
            if (trackingId.isNotEmpty()) {
                googleMap?.clear()
                createAndStartAssetSubscriber(trackingId)
                changeStartButtonState(true)
            } else {
                showToast("Insert tracking ID")
            }
        }
    }

    private fun createAndStartAssetSubscriber(trackingId: String) {
        subscriber = Subscriber.subscribers()
            .connection(ConnectionConfiguration(ABLY_API_KEY, CLIENT_ID))
            .rawLocations { } // if you prefer to display raw location call showMarkerOnMap() here
            .enhancedLocations({ showMarkerOnMap(it) })
            .trackingId(trackingId)
            .resolution(Resolution(Accuracy.MAXIMUM, desiredInterval = 1000L, minimumDisplacement = 1.0))
            .assetStatus({ updateAssetStatusInfo(it) })
            .start()
    }

    private fun updateAssetStatusInfo(isOnline: Boolean) {
        assetStatusTextView.text =
            getString(if (isOnline) R.string.asset_status_online else R.string.asset_status_offline)
    }

    private fun stopSubscribing() {
        subscriber?.stop()
        subscriber = null
        changeStartButtonState(false)
        marker = null
    }

    private fun showMarkerOnMap(location: Location) {
        googleMap?.apply {
            LatLng(location.latitude, location.longitude).let { position ->
                marker.let { currentMarker ->
                    if (currentMarker == null) {
                        marker = addMarker(
                            MarkerOptions()
                                .position(position)
                                .icon(getMarkerIcon(location.bearing))
                        )
                        moveCamera(CameraUpdateFactory.newLatLngZoom(position, ZOOM_LEVEL_STREETS))
                    } else {
                        currentMarker.setIcon(getMarkerIcon(location.bearing))
                        if (animationSwitch.isChecked) {
                            animateMarkerMovement(currentMarker, position)
                        } else {
                            currentMarker.position = position
                        }
                    }
                }
            }
        }
    }

    private fun getMarkerIcon(bearing: Float) =
        BitmapDescriptorFactory.fromResource(getMarkerResourceIdByBearing(bearing))

    private fun changeStartButtonState(isSubscribing: Boolean) {
        if (isSubscribing) {
            startButton.text = getString(R.string.start_button_working)
            startButton.setBackgroundResource(R.drawable.rounded_working)
        } else {
            startButton.text = getString(R.string.start_button_ready)
            startButton.setBackgroundResource(R.drawable.rounded_ready)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
