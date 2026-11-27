package com.trm.sightline

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.ui.graphics.vector.ImageVector
import com.trm.sightline.core.resources.MR
import com.trm.sightline.core.resources.camera_label
import com.trm.sightline.core.resources.map_label
import dev.icerock.moko.resources.StringResource

enum class MainPage(val icon: ImageVector, val label: StringResource) {
  Camera(Icons.Default.PhotoCamera, MR.strings.camera_label),
  Map(Icons.Default.Map, MR.strings.map_label),
}
