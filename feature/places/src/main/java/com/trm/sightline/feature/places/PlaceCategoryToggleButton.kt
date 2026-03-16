package com.trm.sightline.feature.places

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.trm.sightline.core.model.LoadingState
import com.trm.sightline.core.model.Place
import com.trm.sightline.core.model.PlaceCategory

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun RowScope.PlaceCategoryToggleButton(
  category: PlaceCategory,
  icon: ImageVector,
  isSelected: Boolean,
  loadingState: LoadingState<List<Place>>?,
  onClick: (PlaceCategory) -> Unit,
) {
  ToggleButton(
    checked = isSelected,
    onCheckedChange = { onClick(category) },
    modifier = Modifier.weight(1f),
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.padding(vertical = 8.dp),
    ) {
      AnimatedContent(targetState = loadingState) { state ->
        if (state is LoadingState.Loading) {
          CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            trackColor = ToggleButtonDefaults.toggleButtonColors().checkedContentColor,
          )
        } else {
          Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(32.dp))
        }
      }

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
