package com.trm.sightline.feature.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.Man
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.trm.sightline.core.model.MapCameraPosition
import com.trm.sightline.core.model.Place
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.asNumber
import org.maplibre.compose.expressions.dsl.asString
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.convertToNumber
import org.maplibre.compose.expressions.dsl.feature
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.dsl.not
import org.maplibre.compose.expressions.dsl.plus
import org.maplibre.compose.expressions.dsl.step
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.GeoJsonOptions
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import kotlin.math.abs

@Composable
fun MapPreview(
  places: List<Place>,
  lastMapPosition: MapCameraPosition?,
  padding: PaddingValues,
  modifier: Modifier = Modifier,
  onMapPositionChanged: (MapCameraPosition) -> Unit,
) {
  val scope = rememberCoroutineScope()

  val placesBoundingBox = rememberPlacesBoundingBox(places = places, percentageIncrease = 0.1)
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

  LaunchedEffect(placesBoundingBox, padding) {
    if (placesBoundingBox != null) {
      cameraState.animateTo(boundingBox = placesBoundingBox, padding = padding)
    }
  }

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

  LifecycleResumeEffect(Unit) {
    onPauseOrDispose {
      with(cameraState.position) {
        onMapPositionChanged(
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
    MaplibreMap(
      modifier = Modifier.fillMaxSize(),
      baseStyle =
        BaseStyle.Uri(
          "https://tiles.openfreemap.org/styles/${if (isSystemInDarkTheme()) OpenFreeMapStyle.Dark.name.lowercase() else OpenFreeMapStyle.Liberty.name.lowercase()}"
        ),
      options = MapOptions(ornamentOptions = OrnamentOptions.AllDisabled),
      cameraState = cameraState,
      boundingBox = placesBoundingBox,
    ) {
      if (places.isEmpty()) return@MaplibreMap

      val source =
        rememberGeoJsonSource(
          data =
            remember(places) {
              GeoJsonData.Features(
                FeatureCollection(
                  places.map {
                    Feature(
                      id = JsonPrimitive(it.id.toString()),
                      geometry = Point(Position(longitude = it.longitude, latitude = it.latitude)),
                      properties = Unit,
                    )
                  }
                )
              )
            },
          options =
            remember {
              GeoJsonOptions(
                cluster = true,
                clusterRadius = 32,
                clusterMaxZoom = 16,
                clusterProperties =
                  mapOf(
                    "total_range" to
                      GeoJsonOptions.ClusterPropertyAggregator(
                        mapper = feature["current_range_meters"].convertToNumber(),
                        reducer =
                          feature.accumulated().asNumber() +
                            feature["total_range"].convertToNumber(),
                      )
                  ),
              )
            },
        )

      CircleLayer(
        id = "clustered-markers",
        source = source,
        filter = feature.has("point_count"),
        color = const(Color.Green),
        opacity = const(0.5f),
        radius =
          step(
            input = feature["point_count"].asNumber(),
            fallback = const(15.dp),
            25 to const(20.dp),
            100 to const(30.dp),
            500 to const(40.dp),
            1000 to const(50.dp),
            5000 to const(60.dp),
          ),
      )

      SymbolLayer(
        id = "clustered-markers-count",
        source = source,
        filter = feature.has("point_count"),
        textField = feature["point_count_abbreviated"].asString(),
        textFont = const(listOf("Noto Sans Regular")),
        textColor = const(MaterialTheme.colorScheme.onBackground),
      )

      SymbolLayer(
        id = "unclustered-markers",
        source = source,
        filter = !feature.has("point_count"),
        iconImage =
          image(value = rememberVectorPainter(Icons.Filled.Man), size = DpSize(14.dp, 14.dp)),
        iconAllowOverlap = const(true),
      )
    }

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

@Composable
fun rememberPlacesBoundingBox(places: List<Place>, percentageIncrease: Double = 0.0): BoundingBox? =
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

private enum class OpenFreeMapStyle {
  Bright,
  Liberty,
  Positron,
  Dark,
  Fiord,
}
