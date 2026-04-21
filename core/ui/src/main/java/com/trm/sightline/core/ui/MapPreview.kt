package com.trm.sightline.core.ui

import android.location.Location
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.trm.sightline.core.model.Place
import java.io.BufferedReader
import kotlinx.serialization.json.JsonPrimitive
import org.maplibre.compose.camera.CameraState
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
import org.maplibre.compose.map.GestureOptions
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

@Composable
fun MapPreview(
  cameraState: CameraState,
  placesBoundingBox: BoundingBox?,
  places: List<Place>,
  modifier: Modifier = Modifier,
  currentLocation: Location? = null,
) {
  MaplibreMap(
    modifier = modifier,
    baseStyle =
      BaseStyle.Json(
        LocalResources.current
          .openRawResource(if (isSystemInDarkTheme()) R.raw.dark_style else R.raw.light_style)
          .bufferedReader()
          .use(BufferedReader::readText)
      ),
    options =
      MapOptions(
        gestureOptions = GestureOptions.RotationLocked,
        ornamentOptions = OrnamentOptions.AllDisabled,
      ),
    cameraState = cameraState,
    boundingBox = placesBoundingBox,
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
}
