package com.trm.sightline

import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.trm.sightline.core.ar.model.Marker
import com.trm.sightline.core.ar.model.SimpleARMarker
import com.trm.sightline.feature.camera.CameraPreview
import com.trm.sightline.feature.camera.rememberCameraPermissionState
import com.trm.sightline.ui.theme.SightlineTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      SightlineTheme {
        val cameraPermissionState = rememberCameraPermissionState()
        LaunchedEffect(Unit) {
          if (!cameraPermissionState.isGranted) {
            cameraPermissionState.launchRequest()
          }
        }

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center,
          ) {
            AnimatedContent(cameraPermissionState.isGranted) {
              if (it) {
                CameraPreview(
                  location =
                    remember {
                      Location(null).apply {
                        latitude = 52.237049
                        longitude = 21.017532
                      }
                    },
                  markers =
                    remember {
                      List(10) { index ->
                        SimpleARMarker(
                          Marker(
                            "Marker ${index + 1}",
                            Location(null).apply {
                              latitude = 52.237049 + ((index + 1) * 0.001)
                              longitude = 21.017532 + ((index + 1) * 0.001)
                            },
                          )
                        )
                      }
                    },
                )
              } else {
                Button(onClick = cameraPermissionState::launchRequest) {
                  Text(text = "Grant Camera Permission")
                }
              }
            }
          }
        }
      }
    }
  }
}
