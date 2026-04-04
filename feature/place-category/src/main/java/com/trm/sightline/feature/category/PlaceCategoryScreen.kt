package com.trm.sightline.feature.category

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.trm.sightline.core.ar.util.sideSheetWidthDp
import com.trm.sightline.core.common.R as commonR
import com.trm.sightline.core.common.util.rememberBottomSheetScaffoldStateForScreenHeight
import com.trm.sightline.core.model.Place
import com.trm.sightline.core.model.PlaceCategory
import com.trm.sightline.core.ui.rememberBottomSheetExpandedProgress

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SharedTransitionScope.PlaceCategoryScreen(
  route: PlaceCategoryRoute,
  isCompactHeight: Boolean,
  animatedVisibilityScope: AnimatedVisibilityScope,
  onBack: () -> Unit,
  content: @Composable (PaddingValues) -> Unit,
) {
  val scaffoldState = rememberBottomSheetScaffoldStateForScreenHeight(isCompactHeight)
  val sheetState = scaffoldState.bottomSheetState

  val density = LocalDensity.current
  val (sheetNonPeekHeightState, expandedProgressState) =
    rememberBottomSheetExpandedProgress(sheetState)
  var sheetNonPeekHeight by sheetNonPeekHeightState
  val expandedProgress by expandedProgressState

  val sheetContent =
    @Composable { peekHeight: Dp ->
      LazyColumn(
        modifier =
          if (isCompactHeight) {
            Modifier.width(sideSheetWidthDp.dp)
              .fillMaxHeight()
              .systemBarsPadding()
              .navigationBarsPadding()
              .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.End))
              .padding(horizontal = 16.dp)
          } else {
            Modifier.fillMaxWidth()
              .navigationBarsPadding()
              .padding(horizontal = 16.dp)
              .onGloballyPositioned { layoutCoordinates ->
                sheetNonPeekHeight =
                  layoutCoordinates.size.height.toFloat() - with(density) { peekHeight.toPx() }
              }
          },
        contentPadding = PaddingValues(bottom = 16.dp),
      ) {
        item {
          PlaceCategoryHeader(
            category = route.category,
            placesCount = route.places.size,
            animatedVisibilityScope = animatedVisibilityScope,
          )
        }

        items(route.places, key = Place::id) { place -> PlaceListItem(place = place) }
      }
    }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val sheetPeekHeight = if (isCompactHeight) 0.dp else maxHeight / 2
    val sheetContainerColor = BottomSheetDefaults.ContainerColor.copy(alpha = .9f)

    BottomSheetScaffold(
      scaffoldState = scaffoldState,
      sheetPeekHeight = sheetPeekHeight,
      sheetContainerColor = sheetContainerColor,
      sheetDragHandle = {
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Spacer(
            modifier =
              Modifier.height(
                WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding() * expandedProgress
              )
          )
          BottomSheetDefaults.DragHandle()
        }
      },
      sheetContent = { sheetContent(sheetPeekHeight) },
    ) {
      Box(modifier = Modifier.fillMaxSize()) {
        content(
          PaddingValues(
            bottom =
              if (isCompactHeight) {
                WindowInsets.safeDrawing
                  .only(WindowInsetsSides.Bottom)
                  .asPaddingValues()
                  .calculateBottomPadding()
              } else {
                sheetPeekHeight
              },
            end = if (isCompactHeight) sideSheetWidthDp.dp else 0.dp,
          )
        )

        FilledTonalIconButton(
          onClick = onBack,
          colors =
            IconButtonDefaults.filledTonalIconButtonColors(
              containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .5f),
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

        if (isCompactHeight) {
          Surface(
            modifier = Modifier.fillMaxHeight().align(Alignment.CenterEnd),
            color = sheetContainerColor,
            tonalElevation = 1.dp,
          ) {
            sheetContent(0.dp)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
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
        shape = ToggleButtonDefaults.shapes().checkedShape,
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
            imageVector = Icons.Default.Place,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.onSurface,
          )
        }
      }

      Spacer(modifier = Modifier.width(16.dp))

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
