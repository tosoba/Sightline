package com.trm.sightline.feature.category

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import com.trm.sightline.core.model.Place
import com.trm.sightline.core.model.PlaceCategory
import kotlinx.serialization.Serializable
import com.trm.sightline.core.common.R as commonR

@Serializable
data class PlaceCategoryRoute(val category: PlaceCategory, val places: List<Place>) : NavKey

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.PlaceCategoryScreen(
  route: PlaceCategoryRoute,
  modifier: Modifier = Modifier,
  animatedVisibilityScope: AnimatedVisibilityScope,
) {
  Scaffold(modifier = modifier.fillMaxWidth()) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.fillMaxSize().padding(it),
    ) {
      Text(
        text = route.category.label,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
          Modifier.sharedElement(
            sharedContentState = rememberSharedContentState(key = "title-${route.category.name}"),
            animatedVisibilityScope = animatedVisibilityScope,
          ),
      )
      Text(
        text = stringResource(commonR.string.places_count, route.places.size),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
          Modifier.sharedElement(
            sharedContentState = rememberSharedContentState(key = "count-${route.category.name}"),
            animatedVisibilityScope = animatedVisibilityScope,
          ),
      )
    }
  }
}
