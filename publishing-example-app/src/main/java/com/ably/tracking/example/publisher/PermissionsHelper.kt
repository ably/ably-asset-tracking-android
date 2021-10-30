package com.ably.tracking.example.publisher

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions

object PermissionsHelper {
    private val REQUIRED_PERMISSIONS = arrayOf(ACCESS_FINE_LOCATION)
    private const val REQUEST_LOCATION_PERMISSION = 1

    fun requestLocationPermission(activity: Activity) {
        if (!EasyPermissions.hasPermissions(activity, *REQUIRED_PERMISSIONS)) {
            EasyPermissions.requestPermissions(
                activity,
                "Please grant the location permission",
                REQUEST_LOCATION_PERMISSION,
                *REQUIRED_PERMISSIONS
            )
        }
    }

    fun hasFineOrCoarseLocationPermissionGranted(context: Context): Boolean =
        hasPermissionGranted(context, ACCESS_FINE_LOCATION) ||
            hasPermissionGranted(context, ACCESS_COARSE_LOCATION)

    private fun hasPermissionGranted(context: Context, permission: String): Boolean =
        ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        onLocationPermissionGranted: () -> Unit
    ) {
        EasyPermissions.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults,
            object {
                @AfterPermissionGranted(REQUEST_LOCATION_PERMISSION)
                fun onPermissionGranted() {
                    onLocationPermissionGranted()
                }
            }
        )
    }
}
