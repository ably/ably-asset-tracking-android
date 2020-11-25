package com.ably.tracking.example.publisher

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

fun hasFineOrCoarseLocationPermissionGranted(context: Context): Boolean =
    hasPermissionGranted(context, ACCESS_FINE_LOCATION) ||
        hasPermissionGranted(context, ACCESS_COARSE_LOCATION)

fun hasPermissionGranted(context: Context, permission: String): Boolean =
    ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
