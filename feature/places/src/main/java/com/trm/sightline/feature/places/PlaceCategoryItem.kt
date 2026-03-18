package com.trm.sightline.feature.places

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.unit.dp
import com.trm.sightline.core.model.LoadingState
import com.trm.sightline.core.model.Place
import com.trm.sightline.core.model.PlaceCategory

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun PlaceCategoryItem(
  category: PlaceCategory,
  icon: ImageVector,
  isSelected: Boolean,
  loadingState: LoadingState<List<Place>>?,
  modifier: Modifier = Modifier,
  layout: PlacesLayout = PlacesLayout.Row,
  onClick: (PlaceCategory) -> Unit,
  onCategoryClick: (PlaceCategory, List<Place>) -> Unit = { _, _ -> },
) {
  when (layout) {
    PlacesLayout.Row -> {
      val colors =
        ToggleButtonDefaults.toggleButtonColors().run {
          copy(containerColor = containerColor.copy(alpha = .5f))
        }
      ToggleButton(
        checked = isSelected,
        onCheckedChange = { onClick(category) },
        colors = colors,
        modifier = modifier,
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
          modifier = Modifier.padding(vertical = 8.dp),
        ) {
          PlaceCategoryItemIconLoadingState(
            loadingState = loadingState,
            trackColor = colors.checkedContentColor,
            icon = icon,
          )

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = category.name.lowercase().replaceFirstChar(Char::uppercase),
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
      val shapes = ToggleButtonDefaults.shapes()
      val colors =
        ToggleButtonDefaults.toggleButtonColors().run {
          copy(containerColor = containerColor.copy(alpha = .25f))
        }

      Card(
        shape =
          when {
            isPressed -> shapes.pressedShape
            isSelected -> shapes.checkedShape
            else -> shapes.shape
          },
        colors =
          CardDefaults.cardColors(
            containerColor = colors.containerColor.copy(alpha = .25f),
            disabledContainerColor = colors.containerColor.copy(alpha = .25f),
          ),
        modifier = modifier,
        onClick = {
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
            checked = isSelected,
            onCheckedChange = { onClick(category) },
            colors = colors,
            shapes = shapes,
            interactionSource = interactionSource,
            modifier = Modifier.fillMaxHeight().heightIn(min = 72.dp).aspectRatio(1f),
          ) {
            PlaceCategoryItemIconLoadingState(
              loadingState = loadingState,
              trackColor = colors.checkedContentColor,
              icon = icon,
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
              text = category.name.lowercase().replaceFirstChar(Char::uppercase),
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
            )

            AnimatedContent(loadingState) { state ->
              when (state) {
                is LoadingState.Loaded<List<Place>> -> {
                  Text(
                    text = "${state.data.size} places",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
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
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
