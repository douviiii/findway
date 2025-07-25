package com.sopa.findway.compose

import android.Manifest
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.sopa.findway.viewmodel.MapViewModel
import com.google.android.gms.maps.model.LatLng

/**
 * Composable to request location permission and initialize location updates.
 * Calls onLocationReady when location is available.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestLocationPermissionAndInit(
    viewModel: MapViewModel,
    onLocationReady: () -> Unit
) {
    val context = LocalContext.current
    val permissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    LaunchedEffect(permissionState.status.isGranted) {
        if (permissionState.status.isGranted) {
            // Consider moving this logic to the ViewModel for better separation of concerns
            requestSingleLocationUpdate(context, viewModel, onLocationReady)
        } else {
            permissionState.launchPermissionRequest()
        }
    }
}

/**
 * Requests a single location update and updates the ViewModel.
 * This logic could be moved to the ViewModel for better testability and separation.
 */
private fun requestSingleLocationUpdate(
    context: android.content.Context,
    viewModel: MapViewModel,
    onLocationReady: () -> Unit
) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    val locationRequest = LocationRequest.create().apply {
        priority = Priority.PRIORITY_HIGH_ACCURACY
        interval = 10_000
        fastestInterval = 5_000
        numUpdates = 1
    }

    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            viewModel.setCurrentLocation(location.latitude, location.longitude)
            onLocationReady()
        }
    }

    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    ) {
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }
}