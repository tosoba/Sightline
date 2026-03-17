package com.trm.sightline.feature.places

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
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
) {
  val colors =
    ToggleButtonDefaults.toggleButtonColors().run {
      copy(containerColor = containerColor.copy(alpha = .5f))
    }

  when (layout) {
    PlacesLayout.Row -> {
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
      Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        ToggleButton(
          checked = isSelected,
          onCheckedChange = { onClick(category) },
          colors = colors,
          modifier = Modifier.size(64.dp),
        ) {
          PlaceCategoryItemIconLoadingState(
            loadingState = loadingState,
            trackColor = colors.checkedContentColor,
            icon = icon,
          )
        }

        Column {
          Text(
            text = category.name.lowercase().replaceFirstChar(Char::uppercase),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
          )

          AnimatedContent(loadingState) {
            when (it) {
              is LoadingState.Loaded<List<Place>> -> {
                Text(
                  text = "${it.data.size} places",
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                )
              }
              else -> {}
            }
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
