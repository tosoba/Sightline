package com.trm.sightline.core.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.LayoutDirection
import com.trm.sightline.core.model.GeoBounds
import com.trm.sightline.core.model.Place
import org.maplibre.compose.camera.CameraFit
import org.maplibre.compose.map.MapState
import org.maplibre.compose.util.DpPadding
import org.maplibre.spatialk.geojson.BoundingBox

@Composable
fun rememberMapPlacesBoundingBox(
  places: List<Place>,
  percentageIncrease: Double = 0.0,
): BoundingBox? =
  remember(places, percentageIncrease) {
    GeoBounds.paddedAround(places, percentageIncrease)?.toBoundingBox()
  }

private fun GeoBounds.toBoundingBox(): BoundingBox =
  BoundingBox(west = west, south = south, east = east, north = north)

fun PaddingValues.toCameraPadding(): DpPadding =
  DpPadding(
    left = calculateLeftPadding(LayoutDirection.Ltr),
    top = calculateTopPadding(),
    right = calculateRightPadding(LayoutDirection.Ltr),
    bottom = calculateBottomPadding(),
  )

@Composable
fun MapCameraAnimateToBoundingBoxEffect(
  boundingBox: BoundingBox?,
  mapState: MapState,
  padding: PaddingValues,
) {
  LaunchedEffect(boundingBox, padding) {
    if (boundingBox != null) {
      mapState.animateCameraToBounds(
        boundingBox = boundingBox,
        fit = CameraFit(cameraPadding = padding.toCameraPadding()),
      )
    }
  }
}
