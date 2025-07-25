package com.sopa.findway.compose

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.sopa.findway.viewmodel.MapViewModel
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import java.util.Locale
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import kotlinx.coroutines.launch
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.maps.android.compose.Circle

@Composable
private fun SuggestionList(
    suggestions: List<AutocompletePrediction>,
    onSuggestionClick: (AutocompletePrediction) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clip(RoundedCornerShape(12.dp))
            .shadow(4.dp)
    ) {
        items(items = suggestions) { prediction ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSuggestionClick(prediction) }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = prediction.getFullText(null).toString(),
                    color = Color.Black,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun LocationBottomSheet(
    selectedPlaceName: String?,
    selectedLocation: Pair<Double, Double>?,
    onComeToThere: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = selectedPlaceName ?: "",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Lat: ${selectedLocation!!.first}, Lng: ${selectedLocation!!.second}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onComeToThere,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Come to There")
            }
        }
    }
}

@Composable
fun Mapping(modifier: Modifier = Modifier, viewModel: MapViewModel) {
    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsState()
    val selectedLatLng by viewModel.selectedMarkerLatLng.collectAsState()
    val cameraPositionState = rememberCameraPositionState()
    var query by remember { mutableStateOf("") }
    val showSheet = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val selectedLocation by viewModel.selectedLocation.collectAsState()
    val selectedPlaceName by viewModel.selectedPlaceName.collectAsState()
    val routePoints by viewModel.routePoints.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val showStartMarker by viewModel.showStartMarker.collectAsState()

    // Handle location updates and camera movement
    LaunchedEffect(Unit) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                viewModel.startLocationUpdates()
            }
        }
    }

    // Animate camera to destination or current location
    LaunchedEffect(currentLocation, routePoints) {
        if (routePoints.isNotEmpty()) {
            val destination = routePoints.last()
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(destination, 15f)
            )
        } else {
            currentLocation?.let { (lat, lng) ->
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 15f)
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            onMapClick = { latLng ->
                viewModel.setSelectedLocation(latLng.latitude, latLng.longitude)
                val geocoder = Geocoder(context, Locale.getDefault())
                val address = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                if (!address.isNullOrEmpty()) {
                    viewModel.setSelectedPlaceName(address[0].getAddressLine(0))
                } else {
                    viewModel.setSelectedPlaceName("Unknow Location")
                }
                showSheet.value = true
            }
        ) {
            currentLocation?.let { (lat, lng) ->
                Circle(
                    center = LatLng(lat, lng),
                    radius = 20.0, // meters (increased size)
                    fillColor = Color(0xFF2196F3).copy(alpha = 0.7f),
                    strokeColor = Color(0xFF1976D2),
                    strokeWidth = 2f
                )
            }
            selectedLatLng?.let {
                Marker(
                    state = MarkerState(position = it),
                    title = "Destination"
                )
            }
            Polyline(
                points = routePoints,
                color = Color.Blue,
                width = 8f
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(WindowInsets.statusBars.asPaddingValues())
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .align(Alignment.TopCenter),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    viewModel.searchPlace(query)
                },
                placeholder = { Text("Find Location") },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, shape = RoundedCornerShape(50)),
                shape = RoundedCornerShape(50),
                singleLine = true
            )
            if (suggestions.isNotEmpty()) {
                SuggestionList(suggestions = suggestions, onSuggestionClick = { prediction ->
                    query = prediction.getFullText(null).toString()
                    viewModel.getLatLngFromPlaceId(prediction.placeId) { latLng ->
                        latLng?.let {
                            viewModel.setSelectedLocation(it.latitude, it.longitude)
                            viewModel.setSelectedLatLng(it)
                            viewModel.setSelectedPlaceName(prediction.getFullText(null).toString())
                            if (viewModel.origin.value != null) {
                                viewModel.fetchRouteToDestination()
                            }
                        }
                    }
                    viewModel.clearSuggestions()
                })
            }
        }
        if (selectedLocation != null && selectedPlaceName != null) {
            LocationBottomSheet(
                selectedPlaceName = selectedPlaceName,
                selectedLocation = selectedLocation,
                onComeToThere = {
                    viewModel.setDestinationFromSelected()
                    selectedLocation?.let { (lat, lng) ->
                        viewModel.setSelectedLocation(lat, lng)
                        viewModel.setSelectedLatLng(LatLng(lat, lng))
                        viewModel.fetchRouteToDestination()
                    }
                }
            )
        }

        FloatingActionButton(
            onClick = {
                coroutineScope.launch {
                    currentLocation?.let { (lat, lng) ->
                        cameraPositionState.animate(
                            update = CameraUpdateFactory.newLatLngZoom(
                                LatLng(lat, lng),
                                15f
                            )
                        )
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Vị trí của tôi"
            )
        }

        // Add End guide button when route is active
        if (routePoints.isNotEmpty()) {
            Button(
                onClick = { viewModel.endGuide() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            ) {
                Text("End guide")
            }
        }

    }
}
