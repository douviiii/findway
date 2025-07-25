package com.sopa.findway.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val _currentLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val currentLocation: StateFlow<Pair<Double, Double>?> = _currentLocation

    private val _suggestions = MutableStateFlow<List<AutocompletePrediction>>(emptyList())
    val suggestions: StateFlow<List<AutocompletePrediction>> = _suggestions

    private val _selectedMarkerLatLng = MutableStateFlow<LatLng?>(null)
    val selectedMarkerLatLng: StateFlow<LatLng?> = _selectedMarkerLatLng

    private val _selectedPlaceName = MutableStateFlow<String?>(null)
    val selectedPlaceName: StateFlow<String?> = _selectedPlaceName

    private val _selectedLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val selectedLocation: StateFlow<Pair<Double, Double>?> = _selectedLocation

    private val _routePoints = MutableStateFlow<List<LatLng>>(emptyList())
    val routePoints: StateFlow<List<LatLng>> = _routePoints

    private val _origin = MutableStateFlow<LatLng?>(null)
    val origin: StateFlow<LatLng?> = _origin

    private val _destination = MutableStateFlow<LatLng?>(null)
    val destination: StateFlow<LatLng?> = _destination

    private val _showStartMarker = MutableStateFlow(true)
    val showStartMarker: StateFlow<Boolean> = _showStartMarker

    private val placesClient = Places.createClient(application)

    fun setCurrentLocation(lat: Double, lng: Double) {
        val latLng = LatLng(lat, lng)
        _currentLocation.value = Pair(lat, lng)
        _origin.value = latLng
    }

    fun setSelectedLocation(lat: Double, lng: Double) {
        _selectedLocation.value = Pair(lat, lng)
        setSelectedLatLng(LatLng(lat, lng))
    }

    fun setSelectedPlaceName(name: String?) {
        _selectedPlaceName.value = name
    }

    fun setSelectedLatLng(latLng: LatLng) {
        _selectedMarkerLatLng.value = latLng
    }

    fun setDestinationFromSelected() {
        _selectedMarkerLatLng.value?.let { latLng ->
            _destination.value = latLng
            _selectedLocation.value = Pair(latLng.latitude, latLng.longitude)
        }
        _showStartMarker.value = false
    }

    fun searchPlace(query: String) {
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                _suggestions.value = response.autocompletePredictions
            }
            .addOnFailureListener { e ->
                Log.e("Places", "Error fetching predictions: $e")
            }
    }

    fun getLatLngFromPlaceId(placeId: String, onResult: (LatLng?) -> Unit) {
        val request = FetchPlaceRequest.builder(
            placeId,
            listOf(Place.Field.LAT_LNG, Place.Field.ADDRESS)
        ).build()

        placesClient.fetchPlace(request)
            .addOnSuccessListener { result ->
                val latLng = result.place.latLng
                val address = result.place.address
                latLng?.let {
                    _destination.value = it
                    setSelectedPlaceName(address ?: "Unknown Place")
                    onResult(it)
                } ?: onResult(null)
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    fun fetchRouteToDestination() {
        val originLatLng = _origin.value
        val destinationLatLng = _destination.value

        if (originLatLng == null || destinationLatLng == null) return

        val originStr = "${originLatLng.latitude},${originLatLng.longitude}"
        val destinationStr = "${destinationLatLng.latitude},${destinationLatLng.longitude}"

        val applicationContext = getApplication<Application>().applicationContext
        val ai = applicationContext.packageManager
            .getApplicationInfo(applicationContext.packageName, PackageManager.GET_META_DATA)
        val apiKey = ai.metaData.getString("com.google.android.geo.API_KEY")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                        "origin=$originStr&destination=$destinationStr&key=$apiKey"
                val response = URL(url).readText()
                val jsonObject = JSONObject(response)
                Log.d("RouteResponse", response)
                val routes = jsonObject.getJSONArray("routes")
                if (routes.length() > 0) {
                    val overviewPolyline = routes.getJSONObject(0)
                        .getJSONObject("overview_polyline")
                        .getString("points")

                    val decodedPath = decodePolyline(overviewPolyline)
                    withContext(Dispatchers.Main) {
                        _routePoints.value = decodedPath
                    }
                }
            } catch (e: Exception) {
                Log.e("RouteError", "Failed to fetch route: ${e.message}")
            }
        }
    }

    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if ((result and 1) != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if ((result and 1) != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            poly.add(LatLng(lat / 1E5, lng / 1E5))
        }

        return poly
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun startLocationUpdates() {
        val locationClient = LocationServices.getFusedLocationProviderClient(getApplication<Application>())

        val locationRequest = LocationRequest.create().apply {
            interval = 5000
            fastestInterval = 2000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationClient.requestLocationUpdates(
            locationRequest,
            object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation ?: return
                    val latLng = LatLng(location.latitude, location.longitude)
                    _origin.value = latLng
                    if (_destination.value != null) {
                        fetchRouteToDestination()
                    }
                }
            },
            Looper.getMainLooper()
        )
    }

    fun clearSuggestions() {
        _suggestions.value = emptyList()
    }

    fun endGuide() {
        _destination.value = null
        _routePoints.value = emptyList()
        _selectedMarkerLatLng.value = null
        _selectedLocation.value = null
        _selectedPlaceName.value = null
        _showStartMarker.value = true
    }

}