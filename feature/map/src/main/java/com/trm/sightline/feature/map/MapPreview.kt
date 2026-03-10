package com.trm.sightline.feature.map

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.style.BaseStyle

@Composable
fun MapPreview(modifier: Modifier = Modifier) {
  MaplibreMap(
    modifier = modifier,
    baseStyle =
      BaseStyle.Uri(
        "https://tiles.openfreemap.org/styles/${if (isSystemInDarkTheme()) OpenFreeMapStyle.Dark.name.lowercase() else OpenFreeMapStyle.Liberty.name.lowercase()}"
      ),
    options = MapOptions(ornamentOptions = OrnamentOptions.AllDisabled),
  ) {}
}

private enum class OpenFreeMapStyle {
  Bright,
  Liberty,
  Positron,
  Dark,
  Fiord,
}
