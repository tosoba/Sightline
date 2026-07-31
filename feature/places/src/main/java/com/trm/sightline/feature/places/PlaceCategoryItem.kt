package com.trm.sightline.feature.places

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import com.trm.sightline.core.model.LoadingState
import com.trm.sightline.core.model.Place
import com.trm.sightline.core.model.PlaceCategory
import com.trm.sightline.core.common.R as commonR

@Composable
internal fun SharedTransitionScope.PlaceCategoryItem(
  category: PlaceCategory,
  icon: ImageVector,
  enabled: Boolean,
  selected: Boolean,
  loadingState: LoadingState<List<Place>>?,
  alpha: Float,
  modifier: Modifier = Modifier,
  layout: PlacesLayout = PlacesLayout.Row,
  animatedVisibilityScope: AnimatedVisibilityScope,
  onClick: (PlaceCategory) -> Unit,
  onCategoryClick: (PlaceCategory, List<Place>) -> Unit,
) {
  val buttonColors =
    ToggleButtonDefaults.toggleButtonColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = alpha),
      disabledContainerColor =
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = alpha / 2f),
    )

  when (layout) {
    PlacesLayout.Row -> {
      ToggleButton(
        checked = selected,
        onCheckedChange = { onClick(category) },
        enabled = selected || enabled,
        colors = buttonColors,
        modifier = modifier,
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
          modifier = Modifier.padding(vertical = 8.dp),
        ) {
          PlaceCategoryItemIconLoadingState(
            loadingState = loadingState,
            trackColor = buttonColors.checkedContentColor,
            icon = icon,
          )

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = category.label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
          )
        }
      }
    }
    PlacesLayout.Grid -> {
      val interactionSource = remember(::MutableInteractionSource)
      val isPressed by interactionSource.collectIsPressedAsState()

      Card(
        shape =
          when {
            isPressed -> ToggleButtonDefaults.pressedShape
            selected -> ToggleButtonDefaults.checkedShape
            else -> ToggleButtonDefaults.shape
          },
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = alpha),
            disabledContainerColor =
              MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = alpha / 2f),
          ),
        modifier = modifier,
        onClick =
          dropUnlessResumed {
            if (loadingState is LoadingState.Loaded<List<Place>>) {
              onCategoryClick(category, loadingState.data)
            }
          },
        enabled = loadingState is LoadingState.Loaded<List<Place>>,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          ToggleButton(
            checked = selected,
            onCheckedChange = { onClick(category) },
            enabled = selected || enabled,
            colors = buttonColors,
            interactionSource = interactionSource,
            modifier =
              Modifier.sharedBounds(
                  sharedContentState = rememberSharedContentState(key = "icon-${category.name}"),
                  animatedVisibilityScope = animatedVisibilityScope,
                )
                .fillMaxHeight()
                .heightIn(min = 72.dp)
                .padding(8.dp)
                .aspectRatio(1f),
          ) {
            PlaceCategoryItemIconLoadingState(
              loadingState = loadingState,
              trackColor = buttonColors.checkedContentColor,
              icon = icon,
            )
          }

          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
              text = category.label,
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              modifier =
                Modifier.sharedElement(
                    sharedContentState = rememberSharedContentState(key = "title-${category.name}"),
                    animatedVisibilityScope = animatedVisibilityScope,
                  )
                  .basicMarquee(iterations = Int.MAX_VALUE),
            )

            AnimatedContent(loadingState) { state ->
              when (state) {
                is LoadingState.Loaded<List<Place>> -> {
                  Text(
                    text = stringResource(commonR.string.places_count, state.data.size),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier =
                      Modifier.sharedElement(
                          sharedContentState =
                            rememberSharedContentState(key = "count-${category.name}"),
                          animatedVisibilityScope = animatedVisibilityScope,
                        )
                        .basicMarquee(iterations = Int.MAX_VALUE),
                  )
                }
                else -> {}
              }
            }
          }

          Spacer(modifier = Modifier.width(8.dp))

          AnimatedVisibility(loadingState is LoadingState.Loaded<List<Place>>) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.padding(end = 8.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun PlaceCategoryItemIconLoadingState(
  loadingState: LoadingState<List<Place>>?,
  trackColor: Color,
  icon: ImageVector,
) {
  AnimatedContent(targetState = loadingState) { state ->
    when (state) {
      is LoadingState.Loading -> {
        CircularProgressIndicator(modifier = Modifier.size(32.dp), trackColor = trackColor)
      }
      else -> {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(32.dp))
      }
    }
  }
}
