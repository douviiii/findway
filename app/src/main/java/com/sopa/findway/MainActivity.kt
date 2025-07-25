package com.sopa.findway

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.sopa.findway.ui.theme.FindwayTheme
import androidx.activity.viewModels
import com.google.android.libraries.places.api.Places
import com.sopa.findway.compose.Mapping
import com.sopa.findway.compose.RequestLocationPermissionAndInit
import com.sopa.findway.viewmodel.MapViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MapViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {

        val applicationContext = application.applicationContext
        val ai = applicationContext.packageManager
            .getApplicationInfo(applicationContext.packageName, PackageManager.GET_META_DATA)
        val apiKey = ai.metaData.getString("com.google.android.geo.API_KEY")

        Places.initialize(applicationContext, apiKey)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FindwayTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RequestLocationPermissionAndInit(
                        viewModel = viewModel,
                        onLocationReady = {}
                    )
                    Mapping(
                        modifier = Modifier.padding(innerPadding),
                        viewModel
                    )
                }
            }
        }
    }
}

