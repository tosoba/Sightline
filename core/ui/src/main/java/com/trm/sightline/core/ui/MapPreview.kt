package com.trm.sightline.core.ui

import android.location.Location
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.trm.sightline.core.model.Place
import java.io.BufferedReader
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraUpdate
import org.maplibre.compose.expressions.dsl.asNumber
import org.maplibre.compose.expressions.dsl.asString
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.convertToNumber
import org.maplibre.compose.expressions.dsl.feature
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.dsl.not
import org.maplibre.compose.expressions.dsl.plus
import org.maplibre.compose.expressions.dsl.step
import org.maplibre.compose.interaction.ClickResult
import org.maplibre.compose.interaction.MapInteractions
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.map.MapState
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.overlay.MapOverlay
import org.maplibre.compose.overlay.include
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.GeoJsonOptions
import org.maplibre.compose.sources.GeoJsonSourceHandle
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Geometry
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position

@Composable
fun rememberMapPlaceState(
  places: List<Place>,
  currentLocation: Location?,
  initialCameraPosition: CameraPosition = CameraPosition(),
): MapState {
  var clickedCluster by remember { mutableStateOf<Feature<Geometry, JsonObject?>?>(null) }
  val mapState =
    rememberPlacesMapState(
      places = places,
      currentLocation = currentLocation,
      initialCameraPosition = initialCameraPosition,
      onClusterClick = { clickedCluster = it },
    )

  LaunchedEffect(clickedCluster) {
    clickedCluster
      ?.let {
        val handle = mapState.style.sources.filterIsInstance<GeoJsonSourceHandle>().first()
        CameraUpdate(
          target = (it.geometry as Point).coordinates,
          zoom = handle.getClusterExpansionZoom(it),
        )
      }
      ?.let {
        mapState.animateCamera(it)
        clickedCluster = null
      }
  }

  return mapState
}

@Composable
private fun rememberPlacesMapState(
  places: List<Place>,
  currentLocation: Location?,
  initialCameraPosition: CameraPosition,
  onClusterClick: (Feature<Geometry, JsonObject?>) -> Unit,
): MapState =
  rememberMapState(
    initialCameraPosition = initialCameraPosition,
    baseStyle =
      BaseStyle.Json(
        LocalResources.current
          .openRawResource(if (isSystemInDarkTheme()) R.raw.dark_style else R.raw.light_style)
          .bufferedReader()
          .use(BufferedReader::readText)
      ),
  ) {
    if (places.isNotEmpty()) {
      val placesSource =
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
        id = "clustered-places",
        source = placesSource,
        filter = feature.has("point_count"),
        color =
          step(
            input = feature["point_count"].asNumber(),
            fallback = const(MaterialTheme.colorScheme.tertiaryContainer),
            50 to const(MaterialTheme.colorScheme.secondaryContainer),
            100 to const(MaterialTheme.colorScheme.primaryContainer),
          ),
        opacity = const(.9f),
        radius =
          step(
            input = feature["point_count"].asNumber(),
            fallback = const(24.dp),
            50 to const(32.dp),
            100 to const(48.dp),
          ),
        onClick = { features ->
          features.firstOrNull(placesSource::isCluster)?.let {
            onClusterClick(it)
            ClickResult.Consume
          } ?: ClickResult.Pass
        },
      )

      SymbolLayer(
        id = "clustered-places-count",
        source = placesSource,
        filter = feature.has("point_count"),
        textField = feature["point_count_abbreviated"].asString(),
        textFont = const(listOf("Noto Sans Regular")),
        textColor =
          step(
            input = feature["point_count"].asNumber(),
            fallback = const(MaterialTheme.colorScheme.onTertiaryContainer),
            50 to const(MaterialTheme.colorScheme.onSecondaryContainer),
            100 to const(MaterialTheme.colorScheme.onPrimaryContainer),
          ),
        textAllowOverlap = const(true),
      )

      SymbolLayer(
        id = "unclustered-places",
        source = placesSource,
        filter = !feature.has("point_count"),
        iconImage =
          image(
            value =
              painterResource(R.drawable.google_maps)
                .tinted(MaterialTheme.colorScheme.onSurfaceVariant),
            size = DpSize(32.dp, 32.dp),
          ),
        iconAllowOverlap = const(true),
      )
    }

    if (currentLocation != null) {
      SymbolLayer(
        id = "current-location-layer",
        source =
          rememberGeoJsonSource(
            data =
              remember(currentLocation) {
                GeoJsonData.Features(
                  FeatureCollection(
                    listOf(
                      Feature(
                        id = JsonPrimitive("current-location"),
                        geometry =
                          Point(
                            Position(
                              longitude = currentLocation.longitude,
                              latitude = currentLocation.latitude,
                            )
                          ),
                        properties = Unit,
                      )
                    )
                  )
                )
              }
          ),
        iconImage =
          image(
            value =
              rememberVectorPainter(Icons.Default.MyLocation)
                .tinted(MaterialTheme.colorScheme.onSurface),
            size = DpSize(32.dp, 32.dp),
          ),
        iconAllowOverlap = const(true),
      )
    }
  }

@Composable
fun MapPreview(
  mapState: MapState,
  modifier: Modifier = Modifier,
) {
  MaplibreMap(
    modifier = modifier,
    state = mapState,
    interactions = MapInteractions { camera { rotate { enabled = false } } },
    overlay = { include(MapOverlay.None) },
  )
}
