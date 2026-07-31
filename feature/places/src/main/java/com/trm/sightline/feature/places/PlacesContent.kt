package com.trm.sightline.feature.places

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.trm.sightline.core.model.CustomLocation
import com.trm.sightline.core.model.LoadingState
import com.trm.sightline.core.model.Place
import com.trm.sightline.core.model.PlaceCategory
import com.trm.sightline.core.ui.icon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedTransitionScope.PlacesContent(
  places: Map<PlaceCategory, LoadingState<List<Place>>>,
  locationAddress: LoadingState<String>,
  userLocationEnabled: Boolean,
  placeCategoriesEnabled: Boolean,
  customLocationSearchResults: LoadingState<List<CustomLocation>>,
  layout: PlacesLayout,
  alpha: Float,
  animatedVisibilityScope: AnimatedVisibilityScope,
  modifier: Modifier = Modifier,
  onSearchFocusChange: (Boolean) -> Unit,
  onToggleUserLocationEnabled: (Boolean) -> Unit,
  onCustomLocationAddressChange: (String) -> Unit,
  onCustomLocationSearchResultClick: (CustomLocation) -> Unit,
  onTogglePlaceCategory: (PlaceCategory) -> Unit,
  onCategoryClick: (PlaceCategory, List<Place>) -> Unit,
) {
  val focusManager = LocalFocusManager.current
  var isFocused by remember { mutableStateOf(false) }

  Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    val expanded = isFocused && !userLocationEnabled
    SearchBar(
      expanded = expanded,
      onExpandedChange = {},
      modifier = Modifier.fillMaxWidth(),
      colors =
        SearchBarDefaults.colors().run {
          copy(containerColor = containerColor.copy(alpha = alpha))
        },
      windowInsets = WindowInsets(),
      inputField = {
        SearchBarDefaults.InputField(
          query = if (locationAddress is LoadingState.Loaded) locationAddress.data else "",
          onQueryChange = onCustomLocationAddressChange,
          onSearch = {},
          expanded = expanded,
          enabled = !userLocationEnabled,
          onExpandedChange = {},
          placeholder = {
            Text(
              stringResource(
                when {
                  !userLocationEnabled -> R.string.enter_location_placeholder
                  !placeCategoriesEnabled -> R.string.loading_location
                  locationAddress is LoadingState.Loading -> R.string.loading_location_address
                  else -> R.string.location_address_not_found
                }
              )
            )
          },
          modifier =
            Modifier.onFocusChanged {
              isFocused = it.isFocused
              onSearchFocusChange(it.isFocused)
            },
          leadingIcon = {
            when {
              locationAddress is LoadingState.Loading ||
                (userLocationEnabled && !placeCategoriesEnabled) -> {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
              }
              userLocationEnabled && locationAddress is LoadingState.Loaded -> {
                Icon(imageVector = Icons.Default.GpsFixed, contentDescription = null)
              }
              isFocused -> {
                IconButton(onClick = focusManager::clearFocus) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back_content_description),
                  )
                }
              }
              else -> {
                Icon(imageVector = Icons.Default.Search, contentDescription = null)
              }
            }
          },
          trailingIcon = {
            FilledTonalIconToggleButton(
              checked = userLocationEnabled,
              onCheckedChange = onToggleUserLocationEnabled,
            ) {
              Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = stringResource(R.string.my_location_content_description),
              )
            }
          },
        )
      },
    ) {
      LazyColumn {
        when {
          locationAddress is LoadingState.Loaded && locationAddress.data.trim().length < 3 -> {
            item {
              Box(
                modifier = Modifier.fillParentMaxWidth().padding(16.dp).animateItem(),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text =
                    stringResource(
                      if (locationAddress.data.trim().isEmpty()) R.string.start_typing_to_search
                      else R.string.query_too_short
                    ),
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                  textAlign = TextAlign.Center,
                )
              }
            }
          }
          customLocationSearchResults is LoadingState.Loaded -> {
            items(customLocationSearchResults.data) { result ->
              ListItem(
                leadingContent = {
                  Icon(imageVector = Icons.Default.Place, contentDescription = null)
                },
                colors =
                  ListItemDefaults.colors(
                    containerColor = ListItemDefaults.colors().containerColor.copy(alpha = 0f)
                  ),
                modifier =
                  Modifier.animateItem().clickable {
                    focusManager.clearFocus()
                    onCustomLocationSearchResultClick(result)
                  },
              ) {
                Text(text = result.address)
              }
            }
          }
          customLocationSearchResults is LoadingState.Loading -> {
            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth().animateItem()) }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    val rowLayoutCategories = remember {
      PlaceCategory.entries.filter(PlaceCategory::showInRowLayout)
    }
    AnimatedContent(targetState = layout) {
      when (it) {
        PlacesLayout.Row -> {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            rowLayoutCategories.forEach { category ->
              PlaceCategoryItem(
                category = category,
                icon = category.icon,
                enabled = placeCategoriesEnabled,
                selected = category in places,
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
            items(items = PlaceCategory.entries, key = PlaceCategory::name) { category ->
              PlaceCategoryItem(
                category = category,
                icon = category.icon,
                enabled = placeCategoriesEnabled,
                selected = category in places,
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

private val PlaceCategory.showInRowLayout: Boolean
  get() =
    when (this) {
      PlaceCategory.Attractions,
      PlaceCategory.Food,
      PlaceCategory.Accommodation,
      PlaceCategory.Stores -> true
      else -> false
    }
