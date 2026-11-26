package com.trm.sightline.feature.category

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.trm.sightline.core.common.R as commonR
import com.trm.sightline.core.common.util.formattedDistance
import com.trm.sightline.core.model.GeoLocation
import com.trm.sightline.core.model.Place
import com.trm.sightline.core.model.PlaceCategory
import com.trm.sightline.core.ui.AdaptiveSheetScaffold
import com.trm.sightline.core.ui.MapCameraAnimateToBoundingBoxEffect
import com.trm.sightline.core.ui.MapPreview
import com.trm.sightline.core.ui.R as uiR
import com.trm.sightline.core.ui.icon
import com.trm.sightline.core.ui.labelRes
import com.trm.sightline.core.ui.rememberMapPlaceState
import com.trm.sightline.core.ui.rememberMapPlacesBoundingBox
import com.trm.sightline.core.ui.toCameraPadding
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraUpdate
import org.maplibre.spatialk.geojson.Position

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedTransitionScope.PlaceCategoryScreen(
  route: PlaceCategoryRoute,
  location: GeoLocation?,
  isCompactHeight: Boolean,
  animatedVisibilityScope: AnimatedVisibilityScope,
  onBack: () -> Unit,
) {
  val scope = rememberCoroutineScope()
  val mapPlacesBoundingBox =
    rememberMapPlacesBoundingBox(places = route.places, percentageIncrease = 0.1)
  val mapState = rememberMapPlaceState(places = route.places, currentLocation = location)

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val sheetPeekHeight = if (isCompactHeight) 0.dp else maxHeight / 2

    suspend fun animateToPlace(place: Place, mapPadding: PaddingValues) {
      mapState.animateCamera(
        CameraUpdate(
          center = Position(longitude = place.longitude, latitude = place.latitude),
          bearing = mapState.cameraPosition.bearing,
          padding = mapPadding.toCameraPadding(),
          pitch = mapState.cameraPosition.pitch,
          zoom = mapState.cameraPosition.zoom,
        ),
      )
    }

    AdaptiveSheetScaffold(
      isCompactHeight = isCompactHeight,
      sheetPeekHeight = sheetPeekHeight,
      sheetContainerColor = BottomSheetDefaults.ContainerColor.copy(alpha = .9f),
      applyContentPadding = false,
      sheetContentHorizontalPadding = 0.dp,
      sheetContentFillModifier = Modifier.fillMaxWidth(),
      sheetContent = { sheet, sheetModifier ->
        LazyColumn(
          modifier = sheetModifier,
          contentPadding = PaddingValues(bottom = 16.dp),
        ) {
          item {
            PlaceCategoryHeader(
              category = route.category,
              placesCount = route.places.size,
              animatedVisibilityScope = animatedVisibilityScope,
            )
          }

          items(route.places, key = Place::id) { place ->
            PlaceListItem(
              place = place,
              location = location,
              onClick = { scope.launch { animateToPlace(place, sheet.contentPadding) } },
            )
          }
        }
      },
    ) { sheet ->
      Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
          visible =
            LocalLifecycleOwner.current.lifecycle
              .currentStateAsState()
              .value
              .isAtLeast(Lifecycle.State.STARTED),
          enter = fadeIn(),
          exit = fadeOut(),
        ) {
          MapCameraAnimateToBoundingBoxEffect(
            boundingBox = mapPlacesBoundingBox,
            mapState = mapState,
            padding = sheet.contentPadding,
          )

          MapPreview(
            mapState = mapState,
            modifier = Modifier.fillMaxSize(),
          )
        }

        FilledTonalIconButton(
          onClick = onBack,
          colors =
            IconButtonDefaults.filledTonalIconButtonColors(
              containerColor = MaterialTheme.colorScheme.surface,
              contentColor = MaterialTheme.colorScheme.onSurface,
            ),
          modifier =
            Modifier.padding(horizontal = 16.dp).systemBarsPadding().align(Alignment.TopStart),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(commonR.string.back),
          )
        }
      }
    }
  }
}

@Composable
private fun SharedTransitionScope.PlaceCategoryHeader(
  category: PlaceCategory,
  placesCount: Int,
  animatedVisibilityScope: AnimatedVisibilityScope,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.Start) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Surface(
        shape = ToggleButtonDefaults.checkedShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier =
          Modifier.size(72.dp)
            .sharedBounds(
              sharedContentState = rememberSharedContentState(key = "icon-${category.name}"),
              animatedVisibilityScope = animatedVisibilityScope,
            ),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = category.icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.onSurface,
          )
        }
      }

      Spacer(modifier = Modifier.width(16.dp))

      Column {
        Text(
          text = stringResource(category.labelRes),
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onSurface,
          modifier =
            Modifier.sharedElement(
              sharedContentState = rememberSharedContentState(key = "title-${category.name}"),
              animatedVisibilityScope = animatedVisibilityScope,
            ),
        )
        Text(
          text = stringResource(commonR.string.places_count, placesCount),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier =
            Modifier.sharedElement(
              sharedContentState = rememberSharedContentState(key = "count-${category.name}"),
              animatedVisibilityScope = animatedVisibilityScope,
            ),
        )
      }
    }
  }
}

@Composable
private fun PlaceListItem(place: Place, location: GeoLocation?, onClick: () -> Unit) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clickable(onClick = onClick)
        .padding(vertical = 12.dp, horizontal = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Surface(
      shape = MaterialTheme.shapes.medium,
      color = MaterialTheme.colorScheme.surfaceVariant,
      modifier = Modifier.size(64.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Text(
          text = place.initials,
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    Spacer(modifier = Modifier.width(16.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = place.name,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )

      if (location != null) {
        Text(
          text =
            remember(place, location) {
              location.distanceTo(place.location).formattedDistance()
            },
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    val context = LocalContext.current
    FilledTonalIconButton(
      onClick = {
        context.startActivity(
          Intent(
              Intent.ACTION_VIEW,
              "geo:${place.latitude},${place.longitude}?q=${Uri.encode(place.name)}".toUri(),
            )
            .setPackage("com.google.android.apps.maps"),
        )
      },
    ) {
      Icon(
        painter = painterResource(uiR.drawable.google_maps),
        contentDescription = stringResource(commonR.string.open_in_google_maps),
        modifier = Modifier.size(24.dp),
      )
    }
  }
}
