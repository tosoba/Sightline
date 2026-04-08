package com.trm.sightline.feature.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import com.trm.sightline.core.ui.MapCameraAnimateToPlacesBoundingBoxEffect
import com.trm.sightline.core.ui.MapPreview
import com.trm.sightline.core.ui.rememberMapPlacesBoundingBox
import kotlin.math.abs
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.spatialk.geojson.Position

@Composable
fun MapScreen(
  places: List<Place>,
  padding: PaddingValues,
  modifier: Modifier = Modifier,
  lastMapPosition: MapCameraPosition? = null,
  onPause: ((MapCameraPosition) -> Unit)? = null,
) {
  val scope = rememberCoroutineScope()

  val placesBoundingBox = rememberMapPlacesBoundingBox(places = places, percentageIncrease = 0.1)
  val cameraState = rememberCameraState(firstPosition = CameraPosition(padding = padding))
  var initialPositionRestored by rememberSaveable { mutableStateOf(false) }

  val placesCenter =
    remember(placesBoundingBox) {
      placesBoundingBox?.let {
        Position(longitude = (it.west + it.east) / 2.0, latitude = (it.south + it.north) / 2.0)
      }
    }
  val showResetToPlacesBoundingBoxButton =
    remember(cameraState.position.target, placesCenter) {
      placesCenter != null &&
        (abs(cameraState.position.target.latitude - placesCenter.latitude) > 0.0001 ||
          abs(cameraState.position.target.longitude - placesCenter.longitude) > 0.0001)
    }

  MapCameraAnimateToPlacesBoundingBoxEffect(
    placesBoundingBox = placesBoundingBox,
    padding = padding,
    cameraState = cameraState,
  )

  if (placesBoundingBox == null && !initialPositionRestored) {
    LaunchedEffect(lastMapPosition, padding) {
      if (lastMapPosition != null) {
        cameraState.animateTo(
          CameraPosition(
            target = Position(lastMapPosition.longitude, lastMapPosition.latitude),
            zoom = lastMapPosition.zoom,
            bearing = lastMapPosition.bearing,
            tilt = lastMapPosition.tilt,
            padding = padding,
          )
        )
        initialPositionRestored = true
      }
    }
  }

  onPause?.let { action ->
    LifecycleResumeEffect(Unit) {
      onPauseOrDispose {
        with(cameraState.position) {
          action(
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
  }

  Box(modifier = modifier) {
    MapPreview(cameraState = cameraState, placesBoundingBox = placesBoundingBox, places = places)

    AnimatedVisibility(
      visible = showResetToPlacesBoundingBoxButton,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.align(Alignment.BottomEnd).padding(padding).padding(16.dp),
    ) {
      FloatingActionButton(
        onClick = {
          if (placesBoundingBox != null) {
            scope.launch {
              cameraState.animateTo(boundingBox = placesBoundingBox, padding = padding)
            }
          }
        }
      ) {
        Icon(imageVector = Icons.Default.FilterCenterFocus, contentDescription = null)
      }
    }
  }
}
