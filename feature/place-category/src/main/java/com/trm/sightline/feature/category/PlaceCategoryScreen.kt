package com.trm.sightline.feature.category

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.trm.sightline.core.model.Place
import com.trm.sightline.core.model.PlaceCategory
import kotlinx.serialization.Serializable
import com.trm.sightline.core.common.R as commonR

@Serializable
data class PlaceCategoryRoute(val category: PlaceCategory, val places: List<Place>) : NavKey

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SharedTransitionScope.PlaceCategoryScreen(
  route: PlaceCategoryRoute,
  animatedVisibilityScope: AnimatedVisibilityScope,
  modifier: Modifier = Modifier,
  onBack: () -> Unit,
) {
  val sheetState = rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded)
  val scaffoldState = rememberBottomSheetScaffoldState(sheetState)

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val fullHeight = maxHeight
    val peekHeight = fullHeight / 2

    BottomSheetScaffold(
      scaffoldState = scaffoldState,
      sheetPeekHeight = peekHeight,
      sheetDragHandle = { BottomSheetDefaults.DragHandle() },
      sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
      sheetContent = {
        LazyColumn(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
          contentPadding = PaddingValues(bottom = 16.dp),
        ) {
          item {
            PlaceCategoryHeader(
              category = route.category,
              placesCount = route.places.size,
              animatedVisibilityScope = animatedVisibilityScope,
            )
          }

          items(route.places, key = { it.id }) { place -> PlaceListItem(place = place) }
        }
      },
    ) {
      MapSection(onBack = onBack)
    }
  }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.PlaceCategoryHeader(
  category: PlaceCategory,
  placesCount: Int,
  animatedVisibilityScope: AnimatedVisibilityScope,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth().padding(vertical = 16.dp),
    horizontalAlignment = Alignment.Start,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        modifier =
          Modifier.padding(end = 16.dp)
            .sharedBounds(
              sharedContentState = rememberSharedContentState(key = "icon-${category.name}"),
              animatedVisibilityScope = animatedVisibilityScope,
            ),
      ) {
        Icon(
          imageVector = Icons.Default.Place,
          contentDescription = null,
          modifier = Modifier.padding(12.dp).size(24.dp),
          tint = MaterialTheme.colorScheme.primary,
        )
      }

      Column {
        Text(
          text = category.label,
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
private fun PlaceListItem(place: Place) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Surface(
      shape = MaterialTheme.shapes.medium,
      color = MaterialTheme.colorScheme.surfaceVariant,
      modifier = Modifier.size(64.dp),
    ) {
      Icon(
        imageVector = Icons.Default.Place,
        contentDescription = null,
        modifier = Modifier.padding(16.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    Spacer(modifier = Modifier.width(16.dp))

    Text(
      text = place.name,
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}

@Composable
private fun MapSection(onBack: () -> Unit, modifier: Modifier = Modifier) {
  Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceDim)) {
    FilledTonalIconButton(
      onClick = onBack,
      colors =
        IconButtonDefaults.filledTonalIconButtonColors(
          containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .5f),
          contentColor = MaterialTheme.colorScheme.onSurface,
        ),
      modifier = Modifier.padding(horizontal = 16.dp).systemBarsPadding().align(Alignment.TopStart),
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = stringResource(commonR.string.back),
      )
    }

    Text(
      text = "Map Placeholder",
      modifier = Modifier.align(Alignment.Center),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
