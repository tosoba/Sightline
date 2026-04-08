package com.trm.sightline.core.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.trm.sightline.core.model.Place
import org.maplibre.compose.camera.CameraState
import org.maplibre.spatialk.geojson.BoundingBox

@Composable
fun rememberMapPlacesBoundingBox(
  places: List<Place>,
  percentageIncrease: Double = 0.0,
): BoundingBox? =
  remember(places, percentageIncrease) {
    if (places.isEmpty()) return@remember null

    val minLat = places.minOf(Place::latitude)
    val maxLat = places.maxOf(Place::latitude)
    val minLon = places.minOf(Place::longitude)
    val maxLon = places.maxOf(Place::longitude)

    val latDelta = maxLat - minLat
    val lonDelta = maxLon - minLon

    val paddingFactor = percentageIncrease / 2.0

    BoundingBox(
      west = minLon - lonDelta * paddingFactor,
      south = minLat - latDelta * paddingFactor,
      east = maxLon + lonDelta * paddingFactor,
      north = maxLat + latDelta * paddingFactor,
    )
  }

@Composable
fun MapCameraAnimateToPlacesBoundingBoxEffect(
  placesBoundingBox: BoundingBox?,
  padding: PaddingValues,
  cameraState: CameraState,
) {
  LaunchedEffect(placesBoundingBox, padding) {
    if (placesBoundingBox != null) {
      cameraState.animateTo(boundingBox = placesBoundingBox, padding = padding)
    }
  }
}
