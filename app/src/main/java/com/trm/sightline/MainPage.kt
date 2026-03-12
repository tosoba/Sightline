package com.trm.sightline

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.ui.graphics.vector.ImageVector

enum class MainPage(val icon: ImageVector, val label: String) {
  Camera(Icons.Default.PhotoCamera, "Camera"),
  Map(Icons.Default.Map, "Map"),
}
