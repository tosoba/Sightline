package com.trm.sightline.feature.places

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.trm.sightline.core.model.LoadingState
import com.trm.sightline.core.model.Place
import com.trm.sightline.core.model.PlaceCategory

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.PlacesContent(
  state: PlacesState,
  places: Map<PlaceCategory, LoadingState<List<Place>>>,
  layout: PlacesLayout,
  alpha: Float,
  modifier: Modifier = Modifier,
  animatedVisibilityScope: AnimatedVisibilityScope,
  onSearchFocusChange: (Boolean) -> Unit = {},
  onTogglePlaceCategory: (PlaceCategory) -> Unit,
  onCategoryClick: (PlaceCategory, List<Place>) -> Unit,
) {
  val focusManager = LocalFocusManager.current
  var isFocused by remember { mutableStateOf(false) }

  Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    SearchBar(
      expanded = false,
      onExpandedChange = {},
      modifier = Modifier.fillMaxWidth(),
      colors =
        SearchBarDefaults.colors().run {
          copy(containerColor = containerColor.copy(alpha = alpha))
        },
      windowInsets = WindowInsets(),
      inputField = {
        SearchBarDefaults.InputField(
          query = state.location,
          onQueryChange = { state.location = it },
          onSearch = {},
          expanded = false,
          enabled = !state.userLocation,
          onExpandedChange = {},
          placeholder = { Text("Current location") },
          modifier =
            Modifier.onFocusChanged {
              isFocused = it.isFocused
              onSearchFocusChange(it.isFocused)
            },
          leadingIcon = {
            if (isFocused) {
              IconButton(onClick = focusManager::clearFocus) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
              }
            } else {
              Icon(imageVector = Icons.Default.Search, contentDescription = null)
            }
          },
          trailingIcon = {
            FilledTonalIconToggleButton(
              checked = state.userLocation,
              onCheckedChange = { state.userLocation = it },
            ) {
              Icon(imageVector = Icons.Default.MyLocation, contentDescription = "My location")
            }
          },
        )
      },
    ) {}

    Spacer(modifier = Modifier.height(16.dp))

    AnimatedContent(targetState = layout) {
      when (it) {
        PlacesLayout.Row -> {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            PlaceCategory.entries.forEach { category ->
              PlaceCategoryItem(
                category = category,
                icon = category.icon(),
                isSelected = category in places,
                loadingState = places[category],
                layout = it,
                modifier = Modifier.weight(1f),
                alpha = alpha,
                animatedVisibilityScope = animatedVisibilityScope,
                onClick = { onTogglePlaceCategory(category) },
                onCategoryClick = onCategoryClick,
              )
            }
          }
        }
        PlacesLayout.Grid -> {
          LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp),
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            items(PlaceCategory.entries) { category ->
              PlaceCategoryItem(
                category = category,
                icon = category.icon(),
                isSelected = category in places,
                loadingState = places[category],
                layout = it,
                alpha = alpha,
                animatedVisibilityScope = animatedVisibilityScope,
                onClick = { onTogglePlaceCategory(category) },
                onCategoryClick = onCategoryClick,
              )
            }
          }
        }
      }
    }
  }
}

private fun PlaceCategory.icon(): ImageVector =
  when (this) {
    PlaceCategory.ATTRACTIONS -> Icons.Default.Place
    PlaceCategory.FOOD -> Icons.Default.Restaurant
    PlaceCategory.ACCOMMODATION -> Icons.Default.Hotel
    PlaceCategory.STORES -> Icons.Default.Storefront
  }
