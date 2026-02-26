package com.trm.sightline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding),
            contentAlignment = Alignment.Center,
          ) {
            if (cameraPermissionState.isGranted) {
              Text(text = "Camera permission granted!")
            } else {
              Button(onClick = { cameraPermissionState.launchRequest() }) {
                Text(text = "Grant Camera Permission")
              }
            }
          }
        }
      }
    }
  }
}

