package com.trm.sightline.feature.map

import android.location.Location
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.trm.sightline.core.model.MapCameraPosition
import com.trm.sightline.core.model.Place
import com.trm.sightline.core.ui.MapCameraAnimateToBoundingBoxEffect
import com.trm.sightline.core.ui.MapPreview
import com.trm.sightline.core.ui.rememberMapPlaceState
import com.trm.sightline.core.ui.rememberMapPlacesBoundingBox
import com.trm.sightline.core.ui.toCameraPadding
import kotlin.math.abs
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraUpdate
import org.maplibre.spatialk.geojson.Position

@Composable
fun MapScreen(
  currentLocation: Location?,
  places: List<Place>,
  padding: PaddingValues,
  lastMapPosition: MapCameraPosition?,
  modifier: Modifier = Modifier,
  onPause: (MapCameraPosition) -> Unit,
) {
  val scope = rememberCoroutineScope()

  val placesBoundingBox = rememberMapPlacesBoundingBox(places = places, percentageIncrease = 0.1)
  val mapState =
    rememberMapPlaceState(
      places = places,
      currentLocation = currentLocation,
      initialCameraPosition = CameraPosition(padding = padding.toCameraPadding()),
    )
  var initialPositionRestored by rememberSaveable { mutableStateOf(false) }

  val placesCenter =
    remember(placesBoundingBox) {
      placesBoundingBox?.let {
        Position(longitude = (it.west + it.east) / 2.0, latitude = (it.south + it.north) / 2.0)
      }
    }
  val showResetToPlacesBoundingBoxButton =
    remember(mapState.cameraPosition.target, placesCenter) {
      placesCenter != null &&
        mapState.cameraPosition.target.isAwayFrom(
          latitude = placesCenter.latitude,
          longitude = placesCenter.longitude,
        )
    }
  val showResetToCurrentLocationButton =
    remember(mapState.cameraPosition.target, currentLocation) {
      currentLocation != null &&
        mapState.cameraPosition.target.isAwayFrom(
          latitude = currentLocation.latitude,
          longitude = currentLocation.longitude,
        )
    }

  MapCameraAnimateToBoundingBoxEffect(
    boundingBox = placesBoundingBox,
    mapState = mapState,
    padding = padding,
  )

  if (placesBoundingBox == null && !initialPositionRestored) {
    LaunchedEffect(lastMapPosition, padding) {
      if (lastMapPosition != null) {
        mapState.animateCamera(
          CameraUpdate(
            target =
              Position(longitude = lastMapPosition.longitude, latitude = lastMapPosition.latitude),
            zoom = lastMapPosition.zoom,
            bearing = lastMapPosition.bearing,
            tilt = lastMapPosition.tilt,
            padding = padding.toCameraPadding(),
          )
        )
        initialPositionRestored = true
      }
    }
  }

  LifecycleResumeEffect(Unit) {
    onPauseOrDispose {
      with(mapState.cameraPosition) {
        onPause(
          MapCameraPosition(
            latitude = target.latitude,
            longitude = target.longitude,
            zoom = zoom,
            bearing = bearing,
            tilt = tilt,
          )
        )
      }
    }
  }

  Box(modifier = modifier) {
    MapPreview(
      mapState = mapState,
      modifier = Modifier.fillMaxSize(),
    )

    Column(
      modifier = Modifier.align(Alignment.BottomEnd).padding(padding).padding(16.dp),
      horizontalAlignment = Alignment.End,
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      AnimatedVisibility(visible = showResetToCurrentLocationButton) {
        FloatingActionButton(
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          onClick = {
            currentLocation?.let {
              scope.launch {
                mapState.animateCamera(
                  CameraUpdate(
                    target = Position(longitude = it.longitude, latitude = it.latitude),
                    zoom = mapState.cameraPosition.zoom,
                    bearing = mapState.cameraPosition.bearing,
                    tilt = mapState.cameraPosition.tilt,
                    padding = padding.toCameraPadding(),
                  )
                )
              }
            }
          },
        ) {
          Icon(imageVector = Icons.Default.MyLocation, contentDescription = null)
        }
      }

      AnimatedVisibility(visible = showResetToPlacesBoundingBoxButton) {
        FloatingActionButton(
          onClick = {
            if (placesBoundingBox != null) {
              scope.launch {
                mapState.animateCameraToBounds(
                  boundingBox = placesBoundingBox,
                  cameraPadding = padding.toCameraPadding(),
                )
              }
            }
          }
        ) {
          Icon(imageVector = Icons.Default.FilterCenterFocus, contentDescription = null)
        }
      }
    }
  }
}

private fun Position.isAwayFrom(latitude: Double, longitude: Double): Boolean =
  abs(this.latitude - latitude) > MIN_COORDINATE_DELTA ||
    abs(this.longitude - longitude) > MIN_COORDINATE_DELTA

private const val MIN_COORDINATE_DELTA = 0.0001
