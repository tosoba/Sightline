package com.trm.sightline

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.ui.graphics.vector.ImageVector

enum class MainPage(val icon: ImageVector, val labelRes: Int) {
  Camera(Icons.Default.PhotoCamera, R.string.camera_label),
  Map(Icons.Default.Map, R.string.map_label),
}
